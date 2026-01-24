package com.ntu.lottery.controller;

import com.ntu.lottery.common.ApiResponse;
import com.ntu.lottery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// src/main/java/com/ntu/lottery/controller/UserController.java
@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/checkin")
    public ApiResponse<String> checkIn(@RequestParam Long userId,
                                       @RequestParam(required = false, defaultValue = "50") Integer rewardPoints) {
        return ApiResponse.ok(userService.dailyCheckIn(userId, rewardPoints));
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getHistory(@RequestParam Long userId) {
        return ApiResponse.ok(userService.getHistory(userId));
    }

    /**
     * Query current user points.
     */
    @GetMapping("/points")
    public ApiResponse<Integer> points(@RequestParam Long userId) {
        return ApiResponse.ok(userService.getPoints(userId));
    }

    @GetMapping("/leaderboard")
    public ApiResponse<List<Map<String, Object>>> leaderboard(@RequestParam(required = false, defaultValue = "10") Integer limit) {
        return ApiResponse.ok(userService.leaderboard(limit));
    }

    @GetMapping("/invite/submit")
    public ApiResponse<String> submitInviteCode(@RequestParam Long userId, @RequestParam String code) {
        return ApiResponse.ok(userService.submitInviteCode(userId, code));
    }
}