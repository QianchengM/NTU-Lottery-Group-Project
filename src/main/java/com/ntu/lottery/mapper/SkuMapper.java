package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.Sku;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SkuMapper {
    Sku selectById(@Param("id") Long id);

    List<Sku> selectAll();
}
