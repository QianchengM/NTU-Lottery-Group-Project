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
}