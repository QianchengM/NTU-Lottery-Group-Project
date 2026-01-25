package com.ntu.lottery.service;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.mapper.PointsLedgerMapper;
import com.ntu.lottery.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Points domain logic.
 *
 * - DB is the source of truth.
 * - Every points change should be recorded in points_ledger.
 * - Use bizType + bizId as idempotency key.
 */
@Service
public class PointsService {

    private final UserMapper userMapper;
    private final PointsLedgerMapper pointsLedgerMapper;

    public PointsService(UserMapper userMapper, PointsLedgerMapper pointsLedgerMapper) {
        this.userMapper = userMapper;
        this.pointsLedgerMapper = pointsLedgerMapper;
    }

    /**
     * Deduct points for a draw and write ledger.
     *
     * @return generated bizId for this deduction (for audit / later compensation)
     */
    @Transactional
    public String deductForDraw(Long userId, Long activityId, int cost) {
        if (cost <= 0) return null;
        if (userId == null) throw new BusinessException(400, "userId is required");

        // Guard update: prevent negative balance
        int updated = userMapper.deductPointsGuard(userId, cost);
        if (updated <= 0) {
            throw new BusinessException(402, "Insufficient points");
        }

        // Query new balance (cheap)
        int balanceAfter = (int) userMapper.selectPointsById(userId);

        // Unique biz id; if you later want strict idempotency, accept requestId from client.
        String bizId = "draw:" + activityId + ":" + userId + ":" + UUID.randomUUID();

        pointsLedgerMapper.insertLedger(
                userId,
                "DRAW",
                bizId,
                -cost,
                balanceAfter,
                "activityId=" + activityId
        );
        return bizId;
    }
}
