package com.ntu.lottery.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Map;

public interface UserMapper {

    LocalDate selectLastCheckinDate(@Param("id") Long id);

    Integer selectPoints(@Param("id") Long id);

    Map<String, Object> selectByInviteCode(@Param("inviteCode") String inviteCode);

    int addPoints(@Param("id") Long id, @Param("delta") Integer delta);

    int updateCheckin(@Param("id") Long id,
                      @Param("rewardPoints") Integer rewardPoints,
                      @Param("today") LocalDate today);

    int deductPointsGuard(@Param("id") Long id, @Param("cost") Integer cost);
    long selectPointsById(@Param("id") Long id);

    Map<String, Object> selectPointsAndLevel(@Param("id") Long id);
}
