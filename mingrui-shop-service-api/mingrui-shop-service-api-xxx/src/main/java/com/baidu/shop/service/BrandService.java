package com.baidu.shop.service;

import com.baidu.shop.base.Result;
import com.baidu.shop.dto.BrandDTO;
import com.baidu.shop.entity.BrandEntity;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @ClassName BrandService
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/1/22
 * @Version V1.0
 **/
@Api(tags = "品牌接口")
public interface BrandService {

    @ApiOperation(value = "查询商品列表")
    @GetMapping(value = "brand/list")
    Result<PageInfo<BrandEntity>> getBrandInfo(BrandDTO brandDTO);
}
