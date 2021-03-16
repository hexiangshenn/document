package com.baidu.shop.business.impl;

import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.business.OrderService;
import com.baidu.shop.config.JwtConfig;
import com.baidu.shop.dto.Car;
import com.baidu.shop.dto.OrderDTO;
import com.baidu.shop.dto.UserInfo;
import com.baidu.shop.entity.OrderDetailEntity;
import com.baidu.shop.entity.OrderEntity;
import com.baidu.shop.entity.OrderStatusEntity;
import com.baidu.shop.mapper.OrderDetailMapper;
import com.baidu.shop.mapper.OrderMapper;
import com.baidu.shop.mapper.OrderStatusMapper;
import com.baidu.shop.redis.repository.RedisRepository;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.IdWorker;
import com.baidu.shop.utils.JwtUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName OrderServiceImpl
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/15
 * @Version V1.0
 **/
@RestController
public class OrderServiceImpl extends BaseApiService implements OrderService {

    private final String GOODS_CAR_PRE = "goods-car-";

    @Resource
    private JwtConfig jwtConfig;

    @Resource
    private IdWorker idWorker;

    @Resource
    private RedisRepository redisRepository;
    @Resource
    private OrderDetailMapper orderDetailMapper;
    @Resource
    private OrderStatusMapper orderStatusMapper;


    @Resource
    private OrderMapper orderMapper;

    @Override
    @Transactional
    public Result<String> createOrder(OrderDTO orderDTO, String token) {

        long orderId = idWorker.nextId();
        Date date = new Date();
        //解析token
        try {
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, jwtConfig.getPublicKey());
            //准备orderEntity
            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setOrderId(orderId);
//            orderEntity.setTotalPay();
//            orderEntity.setActualPay();
            orderEntity.setPromotionIds("1,2");
            orderEntity.setPaymentType(1);
            orderEntity.setCreateTime(date);
            orderEntity.setUserId(userInfo.getId() + "");
            orderEntity.setBuyerMessage("很好");
            orderEntity.setBuyerNick("郑航");
            orderEntity.setBuyerRate(2);
            orderEntity.setInvoiceType(1);
            orderEntity.setSourceType(1);

            List<Long> priceList = new ArrayList<>();

            //准备orderDetailEntity
            List<OrderDetailEntity> orderDetailEntityList = Arrays.asList(orderDTO.getSkuIds().split(",")).stream().map(skuIdStr -> {
                Car redisCar = redisRepository.getHash(GOODS_CAR_PRE + userInfo.getId(), skuIdStr, Car.class);

                OrderDetailEntity orderDetailEntity = BaiduBeanUtil.copyProperties(redisCar, OrderDetailEntity.class);
                orderDetailEntity.setOrderId(orderId);
                priceList.add(orderDetailEntity.getPrice() * orderDetailEntity.getNum());
                return orderDetailEntity;
            }).collect(Collectors.toList());

            Long totalPrice = priceList.stream().reduce(0L, (oldVal, currentVal) -> oldVal + currentVal);
            orderEntity.setTotalPay(totalPrice);
            orderEntity.setActualPay(totalPrice);
            //准备orderStatusEntity
            OrderStatusEntity orderStatusEntity = new OrderStatusEntity();
            orderStatusEntity.setOrderId(orderId);
            orderStatusEntity.setCreateTime(date);
            orderStatusEntity.setStatus(1);

            //入库
            orderMapper.insertSelective(orderEntity);
            orderDetailMapper.insertList(orderDetailEntityList);
            orderStatusMapper.insertSelective(orderStatusEntity);

            //将当前商品从购物车(redis)中删除掉
            Arrays.asList(orderDTO.getSkuIds().split(",")).stream().forEach(skuIdStr -> {
                redisRepository.delHash(GOODS_CAR_PRE + userInfo.getId(), skuIdStr);
            });
        } catch (Exception e) {
            e.printStackTrace();
            this.setResultError("用户实效");
        }

        return this.setResult(HTTPStatus.OK,"",orderId + "");
    }
}