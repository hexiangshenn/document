package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;

import com.baidu.shop.base.Result;
import com.baidu.shop.dto.BrandDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.entity.CategoryBrandEntity;
import com.baidu.shop.mapper.BrandMapper;
import com.baidu.shop.mapper.CategoryBrandMapper;
import com.baidu.shop.service.BrandService;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.PinYinUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName BrandServiceImpl
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/1/22
 * @Version V1.0
 **/
@RestController
public class BrandServiceImpl extends BaseApiService implements BrandService{

    @Resource
    private BrandMapper brandMapper;

    @Resource
    private CategoryBrandMapper categoryBrandMapper;

    @Override//查询
    public Result<PageInfo<BrandEntity>> getBrandInfo(BrandDTO brandDTO) {
        //分页插件
        PageHelper.startPage(brandDTO.getPage(),brandDTO.getRows());

        if (!StringUtils.isEmpty(brandDTO.getSort())){
            PageHelper.orderBy(brandDTO.getOrderBy());
        }

        BrandEntity brandEntity = BaiduBeanUtil.copyProperties(brandDTO,BrandEntity.class);

        Example example = new Example(BrandEntity.class);
        example.createCriteria().andLike("name","%"+ brandEntity.getName() +"%");

        List<BrandEntity> brandEntityList = brandMapper.selectByExample(example);
        //实例化pageInfo,把查询到的集合放到pageInfo里面,返回给vue
        PageInfo<BrandEntity> pageInfo = new PageInfo<>(brandEntityList);
        return this.setResultSuccess(pageInfo);
    }

    @Transactional
    @Override//新增
    public Result<JSONObject> saveBrandInfo(BrandDTO brandDTO) {
        BrandEntity brandEntity = BaiduBeanUtil.copyProperties(brandDTO, BrandEntity.class);

        brandEntity.setLetter(PinYinUtil.getUpperCase(String.valueOf(brandEntity.getName().toCharArray()[0]),false).toCharArray()[0]);

        brandMapper.insertSelective(brandEntity);

        this.insertCategoryBrandList(brandDTO.getCategories(), brandEntity.getId());
        return this.setResultSuccess();
    }

    @Transactional
    @Override//修改
    public Result<JsonObject> editBrandInfo(BrandDTO brandDTO) {
        BrandEntity brandEntity = BaiduBeanUtil.copyProperties(brandDTO, BrandEntity.class);

        brandEntity.setLetter(PinYinUtil.getUpperCase(String.valueOf(brandEntity.getName().toCharArray()[0]), false).toCharArray()[0]);

        brandMapper.updateByPrimaryKeySelective(brandEntity);

        this.deleteCategoryBrandId(brandEntity.getId());

        this.insertCategoryBrandList(brandDTO.getCategories(),brandEntity.getId());

        return this.setResultSuccess();
    }

    @Transactional
    @Override//删除
    public Result<JsonObject> deleteBrandInfo(Integer id) {
        brandMapper.deleteByPrimaryKey(id);
        this.deleteCategoryBrandId(id);
        return this.setResultSuccess();
    }

    //提取新增中间表
    private void insertCategoryBrandList(String categories,Integer brandId){
        if (StringUtils.isEmpty(categories)) throw new RuntimeException("分类信息不能为空");

        if (categories.contains(",")){
            categoryBrandMapper.insertList(
                    Arrays.asList(categories.split(","))
                    .stream()
                    .map(categoryIdStr -> new CategoryBrandEntity(Integer.valueOf(categoryIdStr),brandId))
                    .collect(Collectors.toList())
            );
        }else{
            CategoryBrandEntity categoryBrandEntity = new CategoryBrandEntity();
            categoryBrandEntity.setBrandId(brandId);
            categoryBrandEntity.setCategoryId(Integer.valueOf(categories));

            categoryBrandMapper.insertSelective(categoryBrandEntity);
        }
    }

    //提取删除
    private void deleteCategoryBrandId(Integer brandId){
        Example example = new Example(CategoryBrandEntity.class);
        example.createCriteria().andEqualTo("brandId",brandId);
        categoryBrandMapper.deleteByExample(example);
    }
}
