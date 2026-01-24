package com.ntu.lottery.service;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.mapper.PrizeMapper;
import com.ntu.lottery.mapper.RecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Random;

// src/main/java/com/ntu/lottery/service/LotteryService.java
@Service
public class LotteryService {
    @Autowired
    private PrizeMapper prizeMapper;

    @Autowired
    private RecordMapper recordMapper;

    public List<Map<String, Object>> listPrizes(Long activityId) {
        if (activityId == null) {
            return prizeMapper.selectAll();
        }
        return prizeMapper.selectByActivityId(activityId);
    }

    @Transactional // 保证抽奖和记录保存同步成功或失败
    public String executeDraw(Long userId, Long activityId) {
        if (userId == null || activityId == null) {
            throw new BusinessException(400, "userId/activityId is required");
        }
        // 1. 获取奖品列表
        List<Map<String, Object>> prizes = prizeMapper.selectByActivityId(activityId);

        if (prizes == null || prizes.isEmpty()) {
            throw new BusinessException(404, "No prizes configured for activityId=" + activityId);
        }

        // 2. 加权随机算法 (原逻辑迁移)
        int random = new Random().nextInt(100) + 1;
        Map<String, Object> selectedPrize = null;
        int currentRate = 0;
        for (Map<String, Object> prize : prizes) {
            Number prob = (Number) prize.get("probability");
            currentRate += prob == null ? 0 : prob.intValue();
            if (random <= currentRate) {
                selectedPrize = prize;
                break;
            }
        }

        if (selectedPrize == null) {
            throw new BusinessException(500, "Prize probability configuration invalid (sum < random range?)");
        }

        String prizeName = (String) selectedPrize.get("name");
        int type = ((Number) selectedPrize.get("type")).intValue();
        Long prizeId = ((Number) selectedPrize.get("id")).longValue();

        // 3. 谢谢惠顾处理
        if (type == 0) {
            saveRecord(userId, activityId, prizeName, type);
            return "很遗憾: " + prizeName;
        }

        // 4. 库存扣减 (乐观锁)
        int rows = prizeMapper.deductStock(prizeId);

        if (rows > 0) {
            saveRecord(userId, activityId, prizeName, type);
            return "恭喜你赢得: " + prizeName;
        }
        return "手慢了，奖品已领完";
    }

    private void saveRecord(Long userId, Long activityId, String prizeName, int type) {
        recordMapper.insertRecord(userId, activityId, prizeName, type);
    }
}