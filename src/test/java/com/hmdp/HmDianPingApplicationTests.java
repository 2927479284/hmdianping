package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class HmDianPingApplicationTests {



    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Autowired
    private RedisIdWorker redisIdWorker;
    @Test
    public void test1(){
        stringRedisTemplate.opsForValue().increment("1");
    }

    @Test
    public void test2(){
        long order = redisIdWorker.nextId("order");
        System.out.println(order);
    }

}
