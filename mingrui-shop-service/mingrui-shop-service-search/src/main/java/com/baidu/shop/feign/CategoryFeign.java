package com.baidu.shop.feign;

import com.baidu.shop.service.CategoryService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @ClassName CategoryFeign
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/6
 * @Version V1.0
 **/
@FeignClient(contextId = "CategoryService",value = "xxx-server")
public interface CategoryFeign extends CategoryService {
}
