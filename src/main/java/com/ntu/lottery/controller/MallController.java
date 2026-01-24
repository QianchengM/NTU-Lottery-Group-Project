package com.ntu.lottery.controller;

import com.ntu.lottery.common.ApiResponse;
import com.ntu.lottery.service.MallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mall")
public class MallController {
    @Autowired
    private MallService mallService; // 同样建议建立 MallService 处理兑换逻辑

    @GetMapping("/exchange")
    public ApiResponse<String> exchange(@RequestParam Long userId, @RequestParam Long prizeId) {
        return ApiResponse.ok(mallService.processExchange(userId, prizeId));
    }
}