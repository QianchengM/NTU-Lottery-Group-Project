package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.ActivityConfig;
import org.apache.ibatis.annotations.Param;

/**
 * Activity configurations.
 *
 * For now we only need draw_cost.
 */
public interface ActivityMapper {

    Integer selectDrawCost(@Param("id") Long activityId);

    ActivityConfig selectConfig(@Param("id") Long activityId);
}
