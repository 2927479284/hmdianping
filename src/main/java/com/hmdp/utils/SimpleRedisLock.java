package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{



    private String name;
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "lock:";

    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";


    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    // 初始化加载脚本文件
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        // new ClassPathResource 默认寻找resource目录 所以直接填写对应的文件名即可
        UNLOCK_SCRIPT.setLocation( new ClassPathResource("unlock.lua"));
        // 设置脚本的返回值类型 这里返回的是 0 1 就用Long类型接收
        UNLOCK_SCRIPT.setResultType(Long.class);
    }



    @Override
    public boolean tryLock(long timeoutSec) {
        String id = ID_PREFIX + Thread.currentThread().getId();
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, id, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(aBoolean);
    }

    /**
     * 新写法 利用lua脚本进行删除 保证了原子性
     */
    @Override
    public void unlock() {
        String id = ID_PREFIX + Thread.currentThread().getId();
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                id);
    }
/*    *//**
     * 老写法 利用 java代码进行删除
     *//*
    @Override
    public void unlock() {
        String id = ID_PREFIX + Thread.currentThread().getId();
        String s = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        if (id.equals(s)){
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }*/
}
