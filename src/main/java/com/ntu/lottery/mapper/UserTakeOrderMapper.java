package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.UserTakeOrder;
import org.apache.ibatis.annotations.Param;

public interface UserTakeOrderMapper {

    UserTakeOrder selectByBizId(@Param("bizId") String bizId);

    int insert(UserTakeOrder order);

    /** 只允许 PROCESSING -> USED，返回 1 表示成功，0 表示已被消费/不存在 */
    int markUsed(@Param("bizId") String bizId);

    java.util.List<UserTakeOrder> scanProcessingTimeout(@Param("beforeTime") java.time.LocalDateTime beforeTime,
                                                        @Param("limit") int limit);
}
