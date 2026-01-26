package com.ntu.lottery.service.risk;

import com.ntu.lottery.common.RedisKeys;
import com.ntu.lottery.entity.RiskBlacklist;
import com.ntu.lottery.mapper.RiskBlacklistMapper;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RiskControlService {

    private final RiskBlacklistMapper riskBlacklistMapper;
    private final RedissonClient redissonClient;

    public RiskControlService(RiskBlacklistMapper riskBlacklistMapper, RedissonClient redissonClient) {
        this.riskBlacklistMapper = riskBlacklistMapper;
        this.redissonClient = redissonClient;
    }

    public boolean isBlacklisted(Long userId) {
        if (userId == null) return false;
        RSet<Long> set = redissonClient.getSet(RedisKeys.blacklistSet());
        if (set.contains(userId)) {
            return true;
        }
        RiskBlacklist db = riskBlacklistMapper.selectByUserId(userId);
        return db != null;
    }
}
