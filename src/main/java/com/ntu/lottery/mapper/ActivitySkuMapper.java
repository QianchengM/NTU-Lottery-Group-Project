package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.ActivitySku;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ActivitySkuMapper {

    List<ActivitySku> selectByActivityId(@Param("activityId") Long activityId);

    int deductStock(@Param("activityId") Long activityId, @Param("skuId") Long skuId);

    int updateStockToZero(@Param("activityId") Long activityId, @Param("skuId") Long skuId,
                          @Param("level") String level);
}
