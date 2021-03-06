package com.baidu.shop.feign;

import com.baidu.shop.service.BrandService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @ClassName BrandFeign
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/6
 * @Version V1.0
 **/
@FeignClient(contextId = "BrandService",value = "xxx-server")
public interface BrandFeign extends BrandService {
}
