package com.ntu.lottery.controller;

import com.ntu.lottery.common.ApiResponse;
import com.ntu.lottery.service.LotteryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DrawController {
    @Autowired
    private LotteryService lotteryService;

    @GetMapping("/prizes")
    public ApiResponse<List<Map<String, Object>>> getPrizes(@RequestParam(required = false) Long activityId) {
        return ApiResponse.ok(lotteryService.listPrizes(activityId));
    }

    @GetMapping("/draw")
    public ApiResponse<String> draw(@RequestParam Long userId, @RequestParam Long activityId) {
        return ApiResponse.ok(lotteryService.executeDraw(userId, activityId));
    }
}