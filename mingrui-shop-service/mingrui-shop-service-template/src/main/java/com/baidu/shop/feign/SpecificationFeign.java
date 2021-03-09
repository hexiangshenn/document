package com.baidu.shop.feign;

import com.baidu.shop.service.SpecificationService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @ClassName SpecificationFeign
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/8
 * @Version V1.0
 **/
@FeignClient(value = "xxx-server",contextId = "SpecificationService")
public interface SpecificationFeign extends SpecificationService{
}
