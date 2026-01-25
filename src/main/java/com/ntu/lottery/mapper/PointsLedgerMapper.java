package com.ntu.lottery.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * Points ledger for audit / reconciliation.
 */
public interface PointsLedgerMapper {

    int insertLedger(@Param("userId") Long userId,
                     @Param("bizType") String bizType,
                     @Param("bizId") String bizId,
                     @Param("delta") Integer delta,
                     @Param("balanceAfter") Integer balanceAfter,
                     @Param("remark") String remark);
}
