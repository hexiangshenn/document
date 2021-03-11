package com.baidu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * @ClassName RunUserServerApplication
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/10
 * @Version V1.0
 **/
@SpringBootApplication
@EnableEurekaClient
@MapperScan(value = "com.baidu.shop.mapper")
public class RunUserServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RunUserServerApplication.class);
    }
}