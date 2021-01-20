package com.baidu.shop.service;
import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.Result;
import com.baidu.shop.entity.CategoryEntity;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName CategoryService
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/1/19
 * @Version V1.0
 **/
@Api(tags = "商品分类接口")
public interface CategoryService {
    @ApiOperation(value = "通过查询商品分类")
    @GetMapping(value = "category/list")
    Result<List<CategoryEntity>> getCategoryByPid(Integer pid);

    @ApiOperation(value = "新增")
    @PostMapping(value = "category/save")
    Result<JsonObject>saveCategory(@RequestBody CategoryEntity categoryEntity);

    @ApiOperation(value = "修改")
    @PutMapping(value = "category/edit")
    Result<JSONObject>editCategory(@RequestBody CategoryEntity categoryEntity);
}
