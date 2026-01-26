package com.ntu.lottery.mapper;

import com.ntu.lottery.entity.UserAwardOrder;
import org.apache.ibatis.annotations.Param;

public interface UserAwardOrderMapper {

    UserAwardOrder selectByTakeBizId(@Param("takeBizId") String takeBizId);

    int insert(UserAwardOrder order);
}
