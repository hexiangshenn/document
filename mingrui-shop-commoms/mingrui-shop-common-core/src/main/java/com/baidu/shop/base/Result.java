package com.baidu.shop.base;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * @ClassName Result
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/1/19
 * @Version V1.0
 **/
@Data
@NoArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    public Result(Integer code,String message,Object data){
        this.code =  code;
        this.message = message;
        this.data = (T) data;
    }
}
