package com.ntu.lottery.service;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.mapper.PrizeMapper;
import com.ntu.lottery.mapper.RecordMapper;
import com.ntu.lottery.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class MallService {

    public static final Long MALL_ACTIVITY_ID = 999L;

    @Autowired
    private PrizeMapper prizeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RecordMapper recordMapper;

    /**
     * Points mall exchange: check points & stock, then deduct points & stock and write a record.
     *
     * This method is transactional so the whole exchange either succeeds or rolls back.
     */
    @Transactional
    public String processExchange(Long userId, Long prizeId) {
        if (userId == null || prizeId == null) {
            throw new BusinessException(400, "userId/prizeId is required");
        }

        // 1) Query prize
        Map<String, Object> prize = prizeMapper.selectById(prizeId);
        if (prize == null || prize.isEmpty()) {
            throw new BusinessException(404, "Prize not found: id=" + prizeId);
        }
        Integer pointCost = (Integer) prize.get("point_cost");
        Integer stock = (Integer) prize.get("stock");
        String prizeName = (String) prize.get("name");

        if (stock == null || stock <= 0) {
            throw new BusinessException(409, "兑换失败: 商品库存不足");
        }
        if (pointCost == null || pointCost < 0) {
            throw new BusinessException(500, "商品未配置 point_cost");
        }

        // 2) Deduct points with guard (avoid negative points)
        int deductPointRows = userMapper.deductPointsGuard(userId, pointCost);
        if (deductPointRows <= 0) {
            Integer cur = userMapper.selectPoints(userId);
            throw new BusinessException(409, "兑换失败: 积分不足 (需要 " + pointCost + ", 当前 " + cur + ")");
        }

        // 3) Deduct stock with guard (avoid negative stock)
        int deductStockRows = prizeMapper.deductStock(prizeId);
        if (deductStockRows <= 0) {
            // force rollback by throwing
            throw new BusinessException(409, "兑换失败: 商品库存不足 (并发)");
        }

        // 4) Save record
        recordMapper.insertRecord(userId, MALL_ACTIVITY_ID, "【商城兑换】" + prizeName, 1);

        return "兑换成功: 消耗 " + pointCost + " 积分，获得 " + prizeName;
    }
}
