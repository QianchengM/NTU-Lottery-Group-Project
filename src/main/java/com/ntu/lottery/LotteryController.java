package com.ntu.lottery;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
public class LotteryController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 1. Get all prizes (View Only)
    @GetMapping("/prizes")
    public List<Map<String, Object>> getAllPrizes() {
        return jdbcTemplate.queryForList("SELECT * FROM prize");
    }

    // 2. The Core Lottery Algorithm (Draw Logic)
    // URL example: http://localhost:8080/draw?userId=1&activityId=1
    @GetMapping("/draw")
    public String draw(@RequestParam Long userId, @RequestParam Long activityId) {
        
        // Step A: Check if User and Activity exist (Optional for simple demo, skipping to save code)

        // Step B: Fetch all prizes for this activity
        String sql = "SELECT * FROM prize WHERE activity_id = ?";
        List<Map<String, Object>> prizes = jdbcTemplate.queryForList(sql, activityId);

        // Step C: Weighted Random Algorithm (The Core Math)
        // 1. Generate a random number between 1 and 100
        int random = new Random().nextInt(100) + 1; 
        
        Map<String, Object> selectedPrize = null;
        int currentRate = 0;

        // 2. Loop through prizes to find the winner
        // Logic: Prize A(20%), Prize B(30%)... 
        // If random=15 (matches A), If random=40 (matches B)
        for (Map<String, Object> prize : prizes) {
            int probability = (Integer) prize.get("probability");
            currentRate += probability;
            if (random <= currentRate) {
                selectedPrize = prize;
                break;
            }
        }

        // Fallback: If logic fails (e.g. probabilities < 100%), default to "Try Again"
        if (selectedPrize == null) {
            return "System Error: Configuration invalid.";
        }

        // Step D: Check Stock & Update Database
        String prizeName = (String) selectedPrize.get("name");
        int type = (Integer) selectedPrize.get("type");
        Long prizeId = (Long) selectedPrize.get("id");

        // If it is NOT a physical prize (Type 0 or 2), no need to check stock strictly
        if (type == 0) {
            saveRecord(userId, activityId, prizeName, type);
            return "Result: " + prizeName; // "Better Luck Next Time"
        }

        // Step E: Optimistic Locking for Stock Deduction (Prevent Overselling)
        // Only update if stock > 0
        String updateSql = "UPDATE prize SET stock = stock - 1 WHERE id = ? AND stock > 0";
        int rows = jdbcTemplate.update(updateSql, prizeId);

        if (rows > 0) {
            // Success: Stock deducted
            saveRecord(userId, activityId, prizeName, type);
            return "Congratulations! You won: " + prizeName;
        } else {
            // Fail: Out of stock (Concurrency or Empty)
            saveRecord(userId, activityId, "Missed (Out of Stock)", 0);
            return "So close! But the prize is out of stock.";
        }
    }

    // Helper method to save history
    private void saveRecord(Long userId, Long activityId, String prizeName, int type) {
        String insertSql = "INSERT INTO record (user_id, activity_id, prize_name, prize_type) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(insertSql, userId, activityId, prizeName, type);
    }
    // ==========================================
    // Admin APIs (For Management Panel)
    // ==========================================

    // 接口 3: 更新奖品库存和概率
    // 例子: /admin/update?id=1&stock=50&probability=100
    @GetMapping("/admin/update")
    public String updatePrize(@RequestParam Long id, 
                              @RequestParam Integer stock, 
                              @RequestParam Integer probability) {
        String sql = "UPDATE prize SET stock = ?, probability = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, stock, probability, id);
        
        if (rows > 0) {
            return "Update Success!";
        } else {
            return "Update Failed: Prize ID not found.";
        }
    }
    // ==========================================
    // 积分商城模块 (Points Mall)
    // ==========================================

    // 接口 4: 积分兑换商品
    // 逻辑: 检查积分 -> 扣积分 -> 扣库存 -> 记流水
    // URL: /mall/exchange?userId=1&prizeId=1
    @GetMapping("/mall/exchange")
    public String exchangePrize(@RequestParam Long userId, @RequestParam Long prizeId) {
        
        // 1. 先查这个商品多少钱，还有没有货
        String queryPrize = "SELECT * FROM prize WHERE id = ?";
        Map<String, Object> prize = jdbcTemplate.queryForMap(queryPrize, prizeId);
        
        int cost = (Integer) prize.get("point_cost");
        int stock = (Integer) prize.get("stock");
        String prizeName = (String) prize.get("name");

        // 2. 查用户有多少积分
        String queryUser = "SELECT points FROM sys_user WHERE id = ?";
        Integer userPoints = jdbcTemplate.queryForObject(queryUser, Integer.class, userId);

        // 3. 核心判断逻辑 
        if (stock <= 0) {
            return "兑换失败: 商品库存不足！";
        }
        if (userPoints < cost) {
            return "兑换失败: 您的积分不足 (需要 " + cost + "，您只有 " + userPoints + ")";
        }

        // 4. 执行交易 (先扣钱，再扣货)
        // 扣积分
        jdbcTemplate.update("UPDATE sys_user SET points = points - ? WHERE id = ?", cost, userId);
        // 扣库存
        jdbcTemplate.update("UPDATE prize SET stock = stock - 1 WHERE id = ?", prizeId);

        // 5. 记录流水
        saveRecord(userId, 999L, "【商城兑换】" + prizeName, 1); // 999代表商城活动ID

        return "兑换成功！您消耗了 " + cost + " 积分，获得了: " + prizeName;
    }
    // ==========================================
    // 增强功能: 签到、排行榜、邀请
    // ==========================================

    // Feature 1: 每日签到 (Daily Check-in)
    // 逻辑: 判断今天是不是已经签到了 -> 没签到就加分
    @GetMapping("/daily/checkin")
    public String dailyCheckIn(@RequestParam Long userId) {
        // 1. 查用户最后一次签到时间
        String sql = "SELECT last_checkin_date FROM sys_user WHERE id = ?";
        // 注意: 这里可能查出来是 null (新用户没签过到)
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, userId);
        
        if (result.isEmpty()) return "User not found";
        
        java.sql.Date lastDate = (java.sql.Date) result.get(0).get("last_checkin_date");
        java.time.LocalDate today = java.time.LocalDate.now();

        // 2. 如果今天已经签到了
        if (lastDate != null && lastDate.toLocalDate().equals(today)) {
            return "Failed: You have already checked in today!";
        }

        // 3. 签到成功: 加 50 分，更新日期
        jdbcTemplate.update("UPDATE sys_user SET points = points + 50, last_checkin_date = ? WHERE id = ?", today, userId);
        
        return "Success! +50 Points added.";
    }

    // Feature 2: 大奖排行榜 (Winners Leaderboard)
    // 逻辑: 只查“实物奖品”(type=1) 的中奖记录，按时间倒序
    @GetMapping("/leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        // 连表查询: 拿到中奖人的名字 (u.username) 和奖品名
        String sql = "SELECT u.username, r.prize_name, r.create_time " +
                     "FROM record r JOIN sys_user u ON r.user_id = u.id " +
                     "WHERE r.prize_type = 1 " +  // 只显示大奖(实物)
                     "ORDER BY r.create_time DESC LIMIT 10";
        return jdbcTemplate.queryForList(sql);
    }

    // Feature 3: 邀请码返利 (Invite Friend)
    // 逻辑: 输入别人的码 -> 别人加分，自己也加分
    @GetMapping("/invite/submit")
    public String submitInviteCode(@RequestParam Long userId, @RequestParam String code) {
        // 1. 不能填自己的码 (假设 Alice 的码是 A001)
        // 实际项目要查库，这里Demo简化一下，防止简单的逻辑漏洞
        
        // 2. 查找这个邀请码是谁的
        String findInviterSql = "SELECT id, username FROM sys_user WHERE invite_code = ?";
        List<Map<String, Object>> inviters = jdbcTemplate.queryForList(findInviterSql, code);

        if (inviters.isEmpty()) {
            return "Invalid Code: Invite code not found.";
        }

        Long inviterId = (Long) inviters.get(0).get("id");
        String inviterName = (String) inviters.get(0).get("username");

        if (inviterId.equals(userId)) {
            return "Failed: You cannot invite yourself!";
        }

        // 3. 双方加分 (奖励机制: 邀请人+100，填写人+50)
        // 给邀请人加分
        jdbcTemplate.update("UPDATE sys_user SET points = points + 100 WHERE id = ?", inviterId);
        // 给自己加分
        jdbcTemplate.update("UPDATE sys_user SET points = points + 50 WHERE id = ?", userId);

        return "Success! You got 50 points, and " + inviterName + " got 100 points.";
    }
    // ==========================================
    // 历史记录模块 (User History)
    // ==========================================

    // 接口 5: 查看我的抽奖流水
    // 逻辑: 查 record 表，按时间倒序排
    @GetMapping("/user/history")
    public List<Map<String, Object>> getUserHistory(@RequestParam Long userId) {
        // 这里的 create_time 数据库存的是 UTC 时间或者服务器时间
        // 查出来返给前端，前端自己格式化
        String sql = "SELECT * FROM record WHERE user_id = ? ORDER BY create_time DESC";
        return jdbcTemplate.queryForList(sql, userId);
    }
}