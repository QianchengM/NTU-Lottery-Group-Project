package com.ntu.lottery.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface RecordMapper {

    int insertRecord(@Param("userId") Long userId,
                     @Param("activityId") Long activityId,
                     @Param("prizeName") String prizeName,
                     @Param("prizeType") Integer prizeType);

    List<Map<String, Object>> selectByUserId(@Param("userId") Long userId);

    List<Map<String, Object>> leaderboard(@Param("limit") Integer limit);
}
