package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.RebateConfig;
import org.apache.ibatis.annotations.Param;

public interface RebateConfigMapper {
    RebateConfig selectByActivityId(@Param("activityId") Long activityId);
}
