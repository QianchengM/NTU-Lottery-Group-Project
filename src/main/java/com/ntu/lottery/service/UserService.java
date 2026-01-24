package com.ntu.lottery.service;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.mapper.RecordMapper;
import com.ntu.lottery.mapper.UserInviteMapper;
import com.ntu.lottery.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private UserInviteMapper userInviteMapper;

    /**
     * Daily check-in: once per day, add points and update last_checkin_date.
     */
    @Transactional
    public String dailyCheckIn(Long userId, int rewardPoints) {
        if (userId == null) throw new BusinessException(400, "userId is required");

        // 用 points 查询判断用户是否存在（避免 selectLastCheckinDate 在不存在时返回 null 的歧义）
        Integer points = userMapper.selectPoints(userId);
        if (points == null) throw new BusinessException(404, "User not found");

        LocalDate last = userMapper.selectLastCheckinDate(userId);
        LocalDate today = LocalDate.now();
        if (last != null && last.equals(today)) {
            throw new BusinessException(409, "Failed: You have already checked in today!");
        }

        int rows = userMapper.updateCheckin(userId, rewardPoints, today);
        if (rows <= 0) {
            throw new BusinessException(500, "Check-in failed");
        }
        return "Success! +" + rewardPoints + " Points added.";
    }

    public List<Map<String, Object>> getHistory(Long userId) {
        if (userId == null) throw new BusinessException(400, "userId is required");
        return recordMapper.selectByUserId(userId);
    }

    public List<Map<String, Object>> leaderboard(int limit) {
        int n = Math.max(1, Math.min(limit, 100));
        return recordMapper.leaderboard(n);
    }

    /**
     * Submit invite code: inviter gets +100, invitee gets +50.
     *
     * Idempotency: a user can bind an invite code only once. We use a separate table `user_invite`
     * with unique constraint on user_id.
     */
    @Transactional
    public String submitInviteCode(Long userId, String code) {
        if (userId == null) throw new BusinessException(400, "userId is required");
        if (code == null || code.isBlank()) throw new BusinessException(400, "code is required");

        // Find inviter
        Map<String, Object> inviter = userMapper.selectByInviteCode(code);
        if (inviter == null || inviter.isEmpty()) {
            throw new BusinessException(404, "Invalid Code: Invite code not found.");
        }
        Long inviterId = ((Number) inviter.get("id")).longValue();
        String inviterName = (String) inviter.get("username");

        if (inviterId.equals(userId)) {
            throw new BusinessException(409, "Failed: You cannot invite yourself!");
        }

        // Idempotency: bind once
        // Requires table: user_invite(user_id PK/UNIQUE, inviter_id, create_time)
        int bindRows = userInviteMapper.bindOnce(userId, inviterId);
        if (bindRows <= 0) {
            throw new BusinessException(409, "Failed: You have already submitted an invite code.");
        }

        // Rewards
        userMapper.addPoints(inviterId, 100);
        userMapper.addPoints(userId, 50);

        return "Success! You got 50 points, and " + inviterName + " got 100 points.";
    }

    public long getPoints(long userId) {
        Long points = userMapper.selectPointsById(userId);
        if (points == null) {
            throw new BusinessException(404, "user not found: " + userId);
        }
        return points;
    }
}
