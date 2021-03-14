package com.baidu.shop.service;

import com.baidu.shop.base.Result;
import com.baidu.shop.dto.Car;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @ClassName CarService
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/13
 * @Version V1.0
 **/
@Api(tags = "购物车接口")
public interface CarService {

    @ApiOperation(value = "添加商品到购物车")
    @PostMapping(value = "car/addCar")
    Result<JSONObject> addCar(@RequestBody Car car, @CookieValue("MRSHOP_TOKEN") String token);

    @ApiOperation(value = "合并购物车")
    @PostMapping(value = "car/mergeCar")
    Result<JSONObject> mergeCar(@RequestBody String carList, @CookieValue("MRSHOP_TOKEN") String token);

    @ApiOperation(value = "获取当前登录用户的购物车信息")
    @GetMapping(value = "car/getUserCar")
    Result<List<Car>> getUserCar(@CookieValue(value = "MRSHOP_TOKEN") String token);

    @ApiOperation(value = "操作购物车列表数量")
    @GetMapping(value = "car/operationNum")
    Result<JSONObject> operationNum(@CookieValue(value = "MRSHOP_TOKEN") String token,Integer type,Long skuId);
}
