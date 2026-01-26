package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.ActivityWeightPrize;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ActivityWeightPrizeMapper {
    List<ActivityWeightPrize> selectByRuleId(@Param("ruleId") Long ruleId);
}
