package com.ntu.lottery.service;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.mapper.RecordMapper;
import com.ntu.lottery.mapper.UserInviteMapper;
import com.ntu.lottery.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private static final int PBKDF2_ITERATIONS = 120000;
    private static final int PBKDF2_KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final int MAX_FAILED_LOGIN = 5;
    private static final int LOCK_MINUTES = 15;

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

    @Transactional
    public Map<String, Object> register(Long userId, String password, String inviteCode) {
        if (userId == null) throw new BusinessException(400, "userId is required");
        if (userId <= 0) throw new BusinessException(400, "userId must be positive");
        if (password == null || password.isBlank()) throw new BusinessException(400, "password is required");
        if (password.length() < 6) throw new BusinessException(400, "password must be at least 6 characters");

        Map<String, Object> existing = userMapper.selectAuthById(userId);
        if (existing != null && !existing.isEmpty()) {
            throw new BusinessException(409, "userId already exists");
        }

        String username = String.valueOf(userId);
        String invite = generateInviteCode();
        String salt = generateSalt();
        String hash = hashPassword(password, salt);

        int rows = userMapper.insertUser(
            userId,
            username,
            hash,
            salt,
            invite,
            1,
            LocalDateTime.now()
        );
        if (rows <= 0) {
            throw new BusinessException(500, "register failed");
        }

        String message = "注册成功";
        if (inviteCode != null && !inviteCode.isBlank()) {
            message = submitInviteCode(userId, inviteCode);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("userId", userId);
        res.put("username", username);
        res.put("inviteCode", invite);
        res.put("message", message);
        return res;
    }

    @Transactional
    public Map<String, Object> login(Long userId, String password) {
        if (userId == null) throw new BusinessException(400, "userId is required");
        if (userId <= 0) throw new BusinessException(400, "userId must be positive");
        if (password == null || password.isBlank()) throw new BusinessException(400, "password is required");

        Map<String, Object> auth = userMapper.selectAuthById(userId);
        if (auth == null || auth.isEmpty()) {
            throw new BusinessException(404, "user not found");
        }

        Integer status = (Integer) auth.get("status");
        if (status != null && status == 0) {
            throw new BusinessException(403, "user is disabled");
        }

        LocalDateTime lockedUntil = toLocalDateTime(auth.get("locked_until"));
        if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
            throw new BusinessException(423, "account locked until " + lockedUntil);
        }

        String salt = (String) auth.get("password_salt");
        String hash = (String) auth.get("password_hash");
        if (salt == null || hash == null) {
            throw new BusinessException(409, "password not set for this account");
        }

        boolean ok = verifyPassword(password, salt, hash);
        if (!ok) {
            userMapper.updateLoginFailure(userId, MAX_FAILED_LOGIN, LOCK_MINUTES);
            throw new BusinessException(401, "invalid password");
        }

        userMapper.updateLoginSuccess(userId, LocalDateTime.now());

        Map<String, Object> res = new HashMap<>();
        res.put("userId", auth.get("id"));
        res.put("username", auth.get("username"));
        res.put("points", auth.get("points"));
        res.put("inviteCode", auth.get("invite_code"));
        res.put("message", "login success");
        return res;
    }

    /**
     * Submit invite code: inviter gets +100, invitee gets +100.
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
        userMapper.addPoints(userId, 100);

        return "Success! You got 100 points, and " + inviterName + " got 100 points.";
    }

    public long getPoints(long userId) {
        Long points = userMapper.selectPointsById(userId);
        if (points == null) {
            throw new BusinessException(404, "user not found: " + userId);
        }
        return points;
    }

    private String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new BusinessException(500, "password hashing failed");
        }
    }

    private boolean verifyPassword(String password, String saltBase64, String expectedHashBase64) {
        String actualHash = hashPassword(password, saltBase64);
        byte[] expected = Base64.getDecoder().decode(expectedHashBase64);
        byte[] actual = Base64.getDecoder().decode(actualHash);
        return MessageDigest.isEqual(expected, actual);
    }

    private String generateInviteCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int i = 0; i < 5; i++) {
            StringBuilder sb = new StringBuilder(8);
            SecureRandom random = new SecureRandom();
            for (int j = 0; j < 8; j++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String code = sb.toString();
            Map<String, Object> existing = userMapper.selectByInviteCode(code);
            if (existing == null || existing.isEmpty()) {
                return code;
            }
        }
        throw new BusinessException(500, "failed to generate invite code");
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        return null;
    }
}
