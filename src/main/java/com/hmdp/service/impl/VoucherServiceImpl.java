package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
    }


    @Autowired
    private ISeckillVoucherService iSeckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    /** 抢购功能初步实现
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
       /* //1.通过id查询对应秒杀券信息
        SeckillVoucher seckillVoucher = iSeckillVoucherService.getById(voucherId);

        //2.判断秒杀是否开始 第一步判断开始时间是否大于现在的时间 大于则代表未开启
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始");
        }
        //3.判断结束时间是否小于当前时间 小于则代表 秒杀已结束
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已结束");
        }
        //4.判断库存是否充足
        if (seckillVoucher.getStock()<1){
            //库存不足
            return Result.fail("抢购失败，库存不足");
        }
        //5.库存充足 开始库存 -1
        boolean voucher_id = seckillVoucherService.update().setSql("stock = stock -1").eq("voucher_id", voucherId).update();
        if (!voucher_id){
            return Result.fail("抢购失败，库存不足");
        }

        //6.抢购完成 新增订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long order = redisIdWorker.nextId("order");
        voucherOrder.setId(order);
        //用户id
        Long userid = UserHolder.getUser().getId();
        voucherOrder.setUserId(userid);
        //代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok();*/
        return null;
    }
}
