package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.ActivityWeightRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ActivityWeightRuleMapper {
    List<ActivityWeightRule> selectByActivityId(@Param("activityId") Long activityId);
}
