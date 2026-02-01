package com.ntu.lottery.service;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.entity.UserAwardOrder;
import com.ntu.lottery.entity.UserTakeOrder;
import com.ntu.lottery.entity.RebateConfig;
import com.ntu.lottery.entity.RebateOrder;
import com.ntu.lottery.mapper.RebateConfigMapper;
import com.ntu.lottery.mapper.RebateOrderMapper;
import com.ntu.lottery.mapper.UserInviteMapper;
import com.ntu.lottery.mapper.UserAwardOrderMapper;
import com.ntu.lottery.mapper.UserTakeOrderMapper;
import com.ntu.lottery.service.reward.RebateDispatchMsg;
import com.ntu.lottery.service.reward.RebateTaskService;
import com.ntu.lottery.service.reward.RewardTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AwardOrderService {

    private final UserTakeOrderMapper userTakeOrderMapper;
    private final UserAwardOrderMapper userAwardOrderMapper;

    /** 你之前实现的：写 task + AFTER_COMMIT 发 MQ */
    private final RewardTaskService rewardTaskService;
    private final RebateConfigMapper rebateConfigMapper;
    private final RebateOrderMapper rebateOrderMapper;
    private final UserInviteMapper userInviteMapper;
    private final RebateTaskService rebateTaskService;

    /**
     * 本地事务B：
     * 1) 保存中奖订单 user_award_order（幂等）
     * 2) 写 Task(outbox) 并 publish event（提交后发 MQ）
     * 3) 更新参与订单 user_take_order = USED（仅允许 PROCESSING->USED）
     */
    @Transactional
    public Long saveAwardAndMarkUsed(Long userId,
                                     Long activityId,
                                     Long prizeId,
                                     String prizeName,
                                     Integer prizeType,
                                     String takeBizId) {

        if (userId == null || activityId == null || prizeId == null || takeBizId == null) {
            throw new BusinessException(400, "missing required params");
        }

        // 0) 校验参与订单存在（事务A产物）
        UserTakeOrder take = userTakeOrderMapper.selectByBizId(takeBizId);
        if (take == null) {
            throw new BusinessException(404, "take order not found: " + takeBizId);
        }
        if (!take.getUserId().equals(userId) || !take.getActivityId().equals(activityId)) {
            throw new BusinessException(409, "take order mismatch");
        }

        // 1) 幂等：若已存在中奖订单，直接返回（避免重复发奖）
        UserAwardOrder existed = userAwardOrderMapper.selectByTakeBizId(takeBizId);
        if (existed != null) {
            return existed.getId();
        }

        // 2) 写中奖订单
        UserAwardOrder award = new UserAwardOrder();
        award.setUserId(userId);
        award.setActivityId(activityId);
        award.setTakeBizId(takeBizId);
        award.setPrizeId(prizeId);
        award.setPrizeName(prizeName);
        award.setPrizeType(prizeType == null ? 0 : prizeType);
        award.setState("CREATE");
        userAwardOrderMapper.insert(award);

        // 3) 写 Task(outbox) + AFTER_COMMIT 发 MQ
        // outBizNo 强烈建议用 awardOrderId 或 takeBizId，这里用 award.getId() 更直观
        String outBizNo = String.valueOf(award.getId());
        rewardTaskService.createSendAwardTaskAndPublish(userId, activityId, prizeId, outBizNo);

        // 3.1) Rebate task (inviter)
        RebateConfig rebateConfig = rebateConfigMapper.selectByActivityId(activityId);
        if (rebateConfig != null && rebateConfig.getStatus() != null && rebateConfig.getStatus() == 1
                && rebateConfig.getRebateValue() != null && rebateConfig.getRebateValue() > 0) {
            Long inviterId = userInviteMapper.selectInviterId(userId);
            if (inviterId != null) {
                String rebateBizId = "rebate:" + activityId + ":" + award.getId();
                RebateOrder existedRebate = rebateOrderMapper.selectByBizId(rebateBizId);
                if (existedRebate == null) {
                    RebateOrder rebateOrder = new RebateOrder();
                    rebateOrder.setUserId(userId);
                    rebateOrder.setInviterId(inviterId);
                    rebateOrder.setActivityId(activityId);
                    rebateOrder.setRebateType(rebateConfig.getRebateType());
                    rebateOrder.setRebateValue(rebateConfig.getRebateValue());
                    rebateOrder.setBizId(rebateBizId);
                    rebateOrder.setState("CREATE");
                    rebateOrderMapper.insert(rebateOrder);

                    RebateDispatchMsg rebateMsg = new RebateDispatchMsg();
                    rebateMsg.setUserId(inviterId);
                    rebateMsg.setActivityId(activityId);
                    rebateMsg.setRebateType(rebateConfig.getRebateType());
                    rebateMsg.setRebateValue(rebateConfig.getRebateValue());
                    rebateMsg.setOutBizNo(rebateBizId);
                    rebateTaskService.createSendRebateTaskAndPublish(rebateMsg, rebateBizId);
                }
            }
        }

        // 4) 更新参与订单为 USED（只允许 PROCESSING->USED）
        int rows = userTakeOrderMapper.markUsed(takeBizId);
        if (rows <= 0) {
            // 走到这里通常意味着：订单已被 used（并发/重复请求）
            // 为了最终一致性：让事务回滚，避免“重复发奖 task”
            throw new BusinessException(409, "take order already used: " + takeBizId);
        }

        return award.getId();
    }
}
