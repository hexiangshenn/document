package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.constant.MrConstants;
import com.baidu.shop.dto.UserDTO;
import com.baidu.shop.entity.UserEntity;
import com.baidu.shop.mapper.UserMapper;
import com.baidu.shop.redis.repository.RedisRepository;
import com.baidu.shop.service.UserService;
import com.baidu.shop.utils.BCryptUtil;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.LuosimaoDuanxinUtil;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

/**
 * @ClassName UserServiceImpl
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/10
 * @Version V1.0
 **/
@RestController
@Slf4j
public class UserServiceImpl extends BaseApiService implements UserService {


    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisRepository redisRepository;

    @Override
    public Result<JSONObject> checkCode(String phone, String code) {
        String redisCode = redisRepository.get(MrConstants.REDIS_DUANXIN_CODE_PRE + phone);
        if(code.equals(redisCode)){
            return this.setResultSuccess();
        }
        return this.setResultError("输入验证码错误");
    }

    @Override
    public Result<JSONObject> register(UserDTO userDTO) {
        UserEntity userEntity = BaiduBeanUtil.copyProperties(userDTO, UserEntity.class);
        userEntity.setPassword(BCryptUtil.hashpw(userEntity.getPassword(),BCryptUtil.gensalt()));
        userEntity.setCreated(new Date());
        userMapper.insertSelective(userEntity);
        return this.setResultSuccess();
    }

    @Override
    public Result<List<UserEntity>> checkUserNameOrPhone(String value, Integer type) {
        Example example = new Example(UserEntity.class);
        /*if(type == 1){
            example.createCriteria().andEqualTo("username",value);
        }else{
            example.createCriteria().andEqualTo("phone",value);
        }*/

        example.createCriteria().andEqualTo(type == 1 ? "username" : "phone",value);
        List<UserEntity> userEntities = userMapper.selectByExample(example);

        return this.setResultSuccess(userEntities);
    }

    @Override
    public Result<JSONObject> sendValidCode(UserDTO userDTO) {
        //验证码
        String code= (int)((Math.random() * 9 + 1) * 100000) + "";
        //手机号
        //发送验证码
        //LuosimaoDuanxinUtil.SendCode(userDTO.getPhone(),code);
        //LuosimaoDuanxinUtil.sendSpeak(userDTO.getPhone(),code);
        log.info("手机号码为 : {} , 验证为 : {}", userDTO.getPhone(),code);
        redisRepository.set(MrConstants.REDIS_DUANXIN_CODE_PRE + userDTO.getPhone(),code);
        redisRepository.expire(MrConstants.REDIS_DUANXIN_CODE_PRE + userDTO.getPhone(),60L);

        return this.setResultSuccess();
    }

}