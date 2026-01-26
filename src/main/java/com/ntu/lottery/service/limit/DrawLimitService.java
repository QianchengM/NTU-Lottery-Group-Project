package com.ntu.lottery.service.limit;

import com.ntu.lottery.common.RedisKeys;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class DrawLimitService {

    private final RedissonClient redissonClient;

    public DrawLimitService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public boolean tryIncrement(Long activityId, Long userId, int limit) {
        if (limit <= 0) return true;
        String day = LocalDate.now().toString().replace("-", "");
        String key = RedisKeys.userDailyDrawCount(activityId, userId, day);
        long ttlSeconds = secondsToEndOfDay();

        String script = """
                local c = redis.call('get', KEYS[1])
                if not c then c = 0 else c = tonumber(c) end
                local limit = tonumber(ARGV[1]) or 0
                local ttl = tonumber(ARGV[2]) or 1
                if (c + 1) > limit then return -1 end
                c = c + 1
                redis.call('set', KEYS[1], c)
                redis.call('expire', KEYS[1], ttl)
                return c
                """;

        Long res = redissonClient.getScript(StringCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE, script, RScript.ReturnType.INTEGER, java.util.List.of(key),
                        String.valueOf(limit), String.valueOf(ttlSeconds));
        return res != null && res > 0;
    }

    private long secondsToEndOfDay() {
        LocalDateTime end = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        return Math.max(1, end.atZone(ZoneId.systemDefault()).toEpochSecond()
                - LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond());
    }
}
