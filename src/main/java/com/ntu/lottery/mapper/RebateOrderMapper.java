package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.RebateOrder;
import org.apache.ibatis.annotations.Param;

public interface RebateOrderMapper {
    RebateOrder selectByBizId(@Param("bizId") String bizId);

    int insert(RebateOrder order);
}
