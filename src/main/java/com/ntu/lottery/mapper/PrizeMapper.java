package com.ntu.lottery.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface PrizeMapper {

    List<Map<String, Object>> selectAll();

    List<Map<String, Object>> selectByActivityId(@Param("activityId") Long activityId);

    Map<String, Object> selectById(@Param("id") Long id);

    int deductStock(@Param("id") Long id);

    int updatePrizeDynamic(@Param("id") Long id,
                           @Param("stock") Integer stock,
                           @Param("probability") Integer probability,
                           @Param("pointCost") Integer pointCost);
}
