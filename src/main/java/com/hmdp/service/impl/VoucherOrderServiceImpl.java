package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService iSeckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;


    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.通过id查询对应秒杀券信息
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
        boolean voucher_id = iSeckillVoucherService.update().setSql("stock = stock -1")
                .eq("voucher_id", voucherId)
                .gt("stock",0)
                .update();
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
        return Result.ok(order);
    }
}
