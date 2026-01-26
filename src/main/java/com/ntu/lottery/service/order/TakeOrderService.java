package com.ntu.lottery.service.order;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.entity.ActivityConfig;
import com.ntu.lottery.entity.UserTakeOrder;
import com.ntu.lottery.mapper.ActivityMapper;
import com.ntu.lottery.mapper.ActivitySkuMapper;
import com.ntu.lottery.mapper.UserTakeOrderMapper;
import com.ntu.lottery.service.PointsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TakeOrderService {

    private final ActivityMapper activityMapper;
    private final ActivitySkuMapper activitySkuMapper;
    private final UserTakeOrderMapper userTakeOrderMapper;
    private final PointsService pointsService;

    public TakeOrderService(ActivityMapper activityMapper,
                            ActivitySkuMapper activitySkuMapper,
                            UserTakeOrderMapper userTakeOrderMapper,
                            PointsService pointsService) {
        this.activityMapper = activityMapper;
        this.activitySkuMapper = activitySkuMapper;
        this.userTakeOrderMapper = userTakeOrderMapper;
        this.pointsService = pointsService;
    }

    @Transactional
    public String createTakeOrder(Long userId, Long activityId, Long skuId, int cost) {
        ActivityConfig config = activityMapper.selectConfig(activityId);
        if (config == null || config.getStatus() == null || config.getStatus() != 1) {
            throw new BusinessException(403, "活动不可用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (config.getStartTime() != null && now.isBefore(config.getStartTime())) {
            throw new BusinessException(403, "活动未开始");
        }
        if (config.getEndTime() != null && now.isAfter(config.getEndTime())) {
            throw new BusinessException(403, "活动已结束");
        }

        if (skuId != null) {
            int rows = activitySkuMapper.deductStock(activityId, skuId);
            if (rows <= 0) {
                throw new BusinessException(409, "库存不足");
            }
        }

        pointsService.deductForDraw(userId, activityId, cost);

        UserTakeOrder order = new UserTakeOrder();
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setBizId("take:" + activityId + ":" + userId + ":" + UUID.randomUUID());
        order.setState("CREATE");
        userTakeOrderMapper.insert(order);

        return order.getBizId();
    }
}
