package com.baidu.shop.mapper;

import com.baidu.shop.entity.UserEntity;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.common.Mapper;

/**
 * @ClassName UserMapper
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/10
 * @Version V1.0
 **/
@Service
public interface UserMapper extends Mapper<UserEntity> {
}