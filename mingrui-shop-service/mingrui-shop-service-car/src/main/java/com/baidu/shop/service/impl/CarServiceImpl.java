package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baidu.config.JwtConfig;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.Car;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.UserInfo;
import com.baidu.shop.entity.SkuEntity;
import com.baidu.shop.feign.GoodsFeign;
import com.baidu.shop.redis.repository.RedisRepository;
import com.baidu.shop.service.CarService;
import com.baidu.shop.utils.JSONUtil;
import com.baidu.shop.utils.JwtUtils;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName CarServiceImpl
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/13
 * @Version V1.0
 **/
@RestController
@Slf4j
public class CarServiceImpl extends BaseApiService implements CarService {

    private final String GOODS_CAR_PRE = "goods-car-";
    private final Integer GOODS_CAR_INCREMENT = 1;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private RedisRepository redisRepository;

    @Autowired
    private GoodsFeign goodsFeign;

    @Override
    public Result<JSONObject> operationNum(String token, Integer type, Long skuId) {

        try {
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, jwtConfig.getPublicKey());
            Car redisCar = redisRepository.getHash("GOODS_CAR_PRE" + userInfo.getId(), skuId + "", Car.class);

            redisCar.setNum(type == GOODS_CAR_INCREMENT ? redisCar.getNum() + 1 : redisCar.getNum() - 1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return this.setResultSuccess();
    }

    @Override
    public Result<List<Car>> getUserCar(String token) {

        List<Object> cars = new ArrayList<>();
        try {
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, jwtConfig.getPublicKey());
            Map<String, String> map = redisRepository.getHash(GOODS_CAR_PRE + userInfo.getId());

            map.forEach((key,value) -> cars.add(JSONUtil.toBean(value,Car.class)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.setResultSuccess(cars);
    }

    @Override
    public Result<JSONObject> mergeCar(String carList, String token) {
        System.out.println(carList);
        com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(carList);
        JSONArray carListJsonArray = jsonObject.getJSONArray("carList");
        List<Car> carsLisr = carListJsonArray.toJavaList(Car.class);

        carsLisr.stream().forEach(car -> {
            this.addCar(car,token);
        });
        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> addCar(Car car,String token) {

        try {

            UserInfo userInfo = JwtUtils.getInfoFromToken(token, jwtConfig.getPublicKey());
            Car redisCar = redisRepository.getHash(GOODS_CAR_PRE + userInfo.getId(), car.getSkuId() + "", Car.class);
            log.info("通过用户id : {} ,skuId : {} 从redis中获取到的数据为 : {}",userInfo.getId(),car.getSkuId(),redisCar);
            if (ObjectUtil.isNotNull(redisCar)){
                redisCar.setNum(redisCar.getNum() + car.getNum());
                redisRepository.setHash(GOODS_CAR_PRE + userInfo.getId(),car.getSkuId() + "", JSONUtil.toJsonString(redisCar));
                log.info("redis中有当前商品 , 重新设置redis中该商品的数量 : {}",redisCar.getNum());
            }else {

                Result<SkuEntity> skuResult = goodsFeign.getSkuById(car.getSkuId());
                if (skuResult.isSuccess()) {
                    SkuEntity skuEntity = skuResult.getData();
                    car.setTitle(skuEntity.getTitle());
                    car.setImage(StringUtils.isEmpty(skuEntity.getImages()) ? "" : skuEntity.getImages().split(",")[0]);
                    car.setPrice(skuEntity.getPrice().longValue());
                    car.setOwnSpec(skuEntity.getOwnSpec());

                    redisRepository.setHash(GOODS_CAR_PRE + userInfo.getId(),car.getSkuId() + "",JSONUtil.toJsonString(car));
                    log.info("redis中没有当前商品 , 新增商品到购物车中 userId : {} , skuId : {} ,商品数据 : {}",userInfo.getId(),car.getSkuId(),JSONUtil.toJsonString(car));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return this.setResultSuccess();
    }
}
