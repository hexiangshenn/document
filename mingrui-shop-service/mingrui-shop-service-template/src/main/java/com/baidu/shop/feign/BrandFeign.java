package com.baidu.shop.feign;

import com.baidu.shop.service.BrandService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @ClassName BrandFeign
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/8
 * @Version V1.0
 **/
@FeignClient(value = "xxx-server",contextId = "BrandService")
public interface BrandFeign extends BrandService {
}
