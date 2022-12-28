package com.hmdp.utils;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker{

    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix){
        // 生成时间戳 当前时间戳-固定时间戳
        LocalDateTime now = LocalDateTime.now();
        long l = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = l - BEGIN_TIMESTAMP;
        //生成序列号 通过redis
        String format = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + format);
        //拼接返回
        return timestamp << COUNT_BITS | increment;
    }

    public static void main(String[] args) {
        long l = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        System.out.println(l);
        System.out.println(l - BEGIN_TIMESTAMP);

        System.out.println(l<<32);
    }
}
