package com.ntu.lottery.mapper;

import org.apache.ibatis.annotations.Param;

public interface UserInviteMapper {

    /**
     * Bind invite relationship once.
     * @return 1 if bound, 0 if already exists
     */
    int bindOnce(@Param("userId") Long userId, @Param("inviterId") Long inviterId);

    Long selectInviterId(@Param("userId") Long userId);
}
