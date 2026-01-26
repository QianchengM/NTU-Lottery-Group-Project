package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.TaskMessage;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskMessageMapper {

    int insert(TaskMessage task);

    TaskMessage selectById(@Param("id") Long id);

    int markSuccess(@Param("id") Long id);

    int markFail(@Param("id") Long id,
                 @Param("lastError") String lastError,
                 @Param("nextRetryTime") LocalDateTime nextRetryTime);

    /** Increase retryCount and push nextRetryTime forward. */
    int touchRetry(@Param("id") Long id,
                   @Param("nextRetryTime") LocalDateTime nextRetryTime);

    List<TaskMessage> scanRetryTasks(@Param("state1") String state1,
                                    @Param("state2") String state2,
                                    @Param("now") LocalDateTime now,
                                    @Param("limit") int limit);
}
