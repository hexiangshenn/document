package com.baidu.shop.service;

import java.util.Map;

/**
 * @ClassName PageService
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/8
 * @Version V1.0
 **/
public interface PageService {
    Map<String,Object> getGoodsInfo(Integer spuId);
}
