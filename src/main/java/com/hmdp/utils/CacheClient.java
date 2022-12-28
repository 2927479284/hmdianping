package com.hmdp.utils;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.utils.RedisData;
import io.netty.util.internal.StringUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);


    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * redis 添加
     *
     * @param key   键名
     * @param value 值
     * @param time  有效期时间
     * @param unit  时间单位(秒 时 等等)
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /** redis 添加一个数据 并自动设置逻辑过期时间 **/
    /**
     * redis 添加一个数据 并自动设置逻辑过期时间
     *
     * @param key   键名
     * @param value 值
     * @param time  有效期时间
     * @param unit  时间单位(秒 时 等等)
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //先设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //开始添加redis数据
        stringRedisTemplate.opsForValue().set(key, String.valueOf(value), time, unit);
    }

    /**
     * 解决缓存穿透问题
     *
     * @param keyPrefix  key前缀
     * @param id         对应id  可以为数字 字符串等等
     * @param type       返回对应类型
     * @param dbFallback 回调方法
     * @param time       存储时间
     * @param unit       时间类型
     * @param <R>        返回结果类型
     * @param <ID>       对应ID类型
     * @return
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        //1.通过传入的key前缀 & 对应ID查询信息
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断数据是否存在
        if (StrUtil.isNotBlank(json)) {
            //存在则返回对应结果
            return JSONUtil.toBean(json, type);
        }
        //3.不存在查询是否为空
        if (json != null) {
            return null;
        }
        //4.查询数据库 这里由调用者传递查询逻辑
        R apply = dbFallback.apply(id);
        //5.如果查询的数据为null 则在redis中写入空数据 防止大量请求发送到数据库
        if (apply == null) {
            stringRedisTemplate.opsForValue().set(key, "", time, unit);
            return null;
        }
        this.set(key, apply, time, unit);
        return apply;
    }

    /**
     * 解决缓存击穿问题
     *
     * @param keyPrefix  key前缀
     * @param id         对应id
     * @param type       返回结果类型
     * @param dbFallback 回调方法函数
     * @param time       有效期
     * @param unit       有效期类型(秒 分 时 等等)
     * @param <R>        返回结果为R
     * @param <ID>       传入ID
     * @return
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        //1.通过key获取对应的redis数据
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        //2.获取到数据查看对应逻辑过期时间是否过期
        if (expireTime.isAfter(LocalDateTime.now())){
            //未过期 返回数据
            return r;
        }
        //3.尝试获取锁 如 获取失败则直接返回过期数据
        String lockKey = LOCK_SHOP_KEY + id;
        boolean b = tryLock(lockKey);
        if (b){
            //4.获取锁成功 则新开线程 完成数据更新操作
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R newR = dbFallback.apply(id);
                    // 重建缓存
                    this.setWithLogicalExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        //获取失败
        //5.返回结果
        return r;
    }

    /**
     * 获取对应的锁
     *
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     *
     * @param key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }


    public static void main(String[] args) {
        StringRedisTemplate stringRedisTemplate1 = new StringRedisTemplate();
        stringRedisTemplate1.opsForValue().increment("1");
    }
}
