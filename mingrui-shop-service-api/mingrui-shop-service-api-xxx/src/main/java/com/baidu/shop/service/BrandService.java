package com.baidu.shop.service;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.BrandDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.validate.group.MingruiOperation;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    Result<PageInfo<BrandEntity>> getBrandInfo(@SpringQueryMap BrandDTO brandDTO);

    @ApiOperation(value = "新增品牌")
    @PostMapping(value = "brand/save")
    Result<JSONObject> saveBrandInfo(@RequestBody BrandDTO brandDTO);

    @ApiOperation(value = "修改品牌")
    @PutMapping(value = "brand/save")
    Result<JsonObject> editBrandInfo(@Validated({MingruiOperation.Update.class}) @RequestBody BrandDTO brandDTO);

    @ApiOperation(value = "删除品牌")
    @DeleteMapping(value = "brand/delete")
    Result<JsonObject> deleteBrandInfo(Integer id);

    @ApiOperation(value = "通过id查询品牌")
    @GetMapping(value = "brand/getBrandInfoByCategoryId")
    Result<List<BrandEntity>> getBrandInfoByCategoryId(Integer cid);

    @ApiOperation(value = "通过品牌id集合获取品牌")
    @GetMapping(value = "bran/getBrandByIds")
    Result<List<BrandEntity>> getBrandByIds(@RequestParam String brandIds);

}
