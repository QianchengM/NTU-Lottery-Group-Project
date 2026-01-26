package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.RiskBlacklist;
import org.apache.ibatis.annotations.Param;

public interface RiskBlacklistMapper {
    RiskBlacklist selectByUserId(@Param("userId") Long userId);
}
