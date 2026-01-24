package com.ntu.lottery.controller;

import com.ntu.lottery.common.ApiResponse;
import com.ntu.lottery.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * Update prize stock/probability/pointCost.
     * Example: /api/admin/prize/update?id=1&stock=50&probability=25&pointCost=200
     */
    @GetMapping("/prize/update")
    public ApiResponse<String> updatePrize(
            @RequestParam Long id,
            @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) Integer probability,
            @RequestParam(required = false, name = "pointCost") Integer pointCost
    ) {
        return ApiResponse.ok(adminService.updatePrize(id, stock, probability, pointCost));
    }

    /**
     * Preheat (assemble) activity cache into Redis.
     * Example: /api/admin/assemble?activityId=1
     */
    @GetMapping("/assemble")
    public ApiResponse<String> assemble(@RequestParam Long activityId) {
        return ApiResponse.ok(adminService.assemble(activityId));
    }
}
