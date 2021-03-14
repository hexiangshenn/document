package com.baidu.shop.service;

import com.baidu.shop.base.Result;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.SkuEntity;
import com.baidu.shop.entity.SpuDetailEntity;
import com.baidu.shop.entity.SpuEntity;
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
 * @ClassName GoodsService
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/2/1
 * @Version V1.0
 **/
@Api(tags = "商品接口")
public interface GoodsService {

    @ApiOperation(value = "获取spu信息")
    @GetMapping(value = "goods/getSpuInfo")
    Result<List<SpuDTO>> getSpuInfo(@SpringQueryMap SpuDTO spuDTO);

    @ApiOperation(value = "新建商品")
    @PostMapping(value = "goods/save")
    Result<JsonObject> saveGoods(@RequestBody SpuDTO spuDTO);

    @ApiOperation(value = "修改商品")
    @PutMapping(value = "goods/save")
    Result<JsonObject> editGoods(@Validated({MingruiOperation.Update.class}) @RequestBody SpuDTO spuDTO);

    @ApiOperation(value = "获取spu详细信息")
    @GetMapping(value = "goods/getSpuDetailBySpu")
    public Result<SpuDetailEntity> getSpuDetailBySpu(@RequestParam Integer spuId);

    @ApiOperation(value = "获取sku信息")
    @GetMapping(value = "goods/getSkuBySpuId")
    Result<List<SkuDTO>> getSkuBySpuId(@RequestParam Integer spuId);

    @ApiOperation(value = "删除商品")
    @DeleteMapping(value = "goods/deleteGoods")
    Result<JsonObject> deleteGoods(Integer spuId);

    @ApiOperation(value = "上下架")
    @PutMapping(value = "goods/upAndDown")
    Result<JsonObject> upAndDown(@RequestBody SpuDTO spuDTO);

    @ApiOperation(value = "通过skuId查询sku信息")
    @GetMapping(value = "/goods/getSkuById")
    Result<SkuEntity> getSkuById(@RequestParam Long skuId);
}