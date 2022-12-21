package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Log4j2
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号是否符合某个规则
        if(RegexUtils.isPhoneInvalid(phone)){
            //2.如果手机号不符合规则 则弹出指定错误提示
            return Result.fail("格式错误");
        }
        //3.符合规则 生成验证码
        String code = RandomUtil.randomNumbers(4);
        //4.将验证码保存至session k->value
        session.setAttribute("code",code);
        //5.发送验证码
        //TODO 未配置第三方 输出日志 模拟发送验证码
        log.error("验证码发送成功：{}",code);
        return Result.ok("验证码发送成功");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号规则
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            //1.1.不符合则返回错误信息
            return Result.fail("格式错误");
        }
        //2.校验验证码
        String code = loginForm.getCode();
        Object code1 = session.getAttribute("code");
        if (code1 == null || !code1.toString().equals(code)){
            //2.1.验证码不一致报错
            return Result.fail("验证码不一致/验证码为空");
        }
        //2.2.验证码一致根据手机号查询对应用户
        User user = query().eq("phone", phone).one();
        //3.判断用户是否存在
        if (user == null){
            //3.1.不存在创建新用户
            user = createUserWithPhone(phone);
        }

        //4.将用户信息保存至session中
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10));
        save(user);
        return user;
    }
}
