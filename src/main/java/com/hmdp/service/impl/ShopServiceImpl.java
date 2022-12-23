package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 1.先从redis查询对应商铺缓存信息
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) throws Exception {
        // 缓存穿透
        //Shop shop = queryWithPassThrough(id);

        // 缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null){
            return Result.fail("店铺信息不存在");
        }
        return Result.ok(shop);
    }


    /**
     * 缓存击穿
     * @param id
     * @return
     */
    private Shop queryWithMutex(Long id) throws Exception{

        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        if (StrUtil.isNotBlank(shopJson)){
            //商铺信息存在于redis缓存中
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        if (shopJson != null){
            return null;
        }
        //缓存未命中尝试获取互斥锁
        String lockKey = "lock:shop:"+id;
        Boolean aBoolean = tryLock(lockKey);
        if (!aBoolean){
            //未获取到互斥锁递归执行本方法
            Thread.sleep(50);
            queryWithMutex(id);
        }
        //不存在则查询数据库
        Shop byId = getById(id);
        if (byId == null){
            //如果查出来是null 也写入redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"",CACHE_SHOP_TTL, TimeUnit.MINUTES);
            unlock(lockKey);
            return null;
        }else {
            //查询出来的数据存在写入redis缓存中
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(byId));
            unlock(lockKey);
            return byId;
        }

    }

    private Boolean tryLock(String key){
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    /**
     * 缓存穿透
     * @param id
     * @return
     */
    private Shop queryWithPassThrough(Long id){

        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        if (StrUtil.isNotBlank(shopJson)){
            //商铺信息存在于redis缓存中
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        if (shopJson != null){
            return null;
        }
        //不存在则查询数据库
        Shop byId = getById(id);
        if (byId == null){
            //如果查出来是null 也写入redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"",CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return null;
        }else {
            //查询出来的数据存在写入redis缓存中
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(byId));
            return byId;
        }

    }




    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
