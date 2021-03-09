package com.baidu.shop.feign;

import com.baidu.shop.service.CategoryService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @ClassName CategoryFeign
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/8
 * @Version V1.0
 **/
@FeignClient(value = "xxx-server",contextId = "CategoryService")
public interface CategoryFeign extends CategoryService {
}
