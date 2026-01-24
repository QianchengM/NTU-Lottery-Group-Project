package com.ntu.lottery.service;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.mapper.PrizeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    @Autowired
    private PrizeMapper prizeMapper;

    @Autowired
    private LotteryAssembleService assembleService;

    /**
     * Partial update: only update fields that are not null.
     */
    public String updatePrize(Long id, Integer stock, Integer probability, Integer pointCost) {
        if (id == null) throw new BusinessException(400, "id is required");
        if (stock == null && probability == null && pointCost == null) {
            throw new BusinessException(400, "at least one of stock/probability/pointCost must be provided");
        }

        int rows = prizeMapper.updatePrizeDynamic(id, stock, probability, pointCost);
        if (rows <= 0) {
            throw new BusinessException(404, "Prize not found: id=" + id);
        }
        return "Update Success";
    }

    /**
     * Preheat (assemble) the activity's cached configs/stock/rate-table into Redis.
     */
    public String assemble(Long activityId) {
        assembleService.assembleActivity(activityId);
        return "Assembled activityId=" + activityId;
    }
}
