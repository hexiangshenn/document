package com.baidu.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName UserInfo
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/12
 * @Version V1.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private Integer id;
    private String username;
}
