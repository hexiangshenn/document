package com.baidu.shop.service.impl;

import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.*;
import com.baidu.shop.mapper.*;
import com.baidu.shop.service.GoodsService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * @ClassName GoodsServiceImpl
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/2/1
 * @Version V1.0
 **/
@RestController
public class GoodsServiceImpl extends BaseApiService implements GoodsService {

    @Resource
    private SpuMapper spuMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private BrandMapper brandMapper;

    @Override
    public Result<List<SpuDTO>> getSpuInfo(SpuDTO spuDTO) {
        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO, SpuEntity.class);

        if(!StringUtils.isEmpty(spuDTO.getSort())){
            PageHelper.orderBy(spuDTO.getOrderBy());
        }

        if (ObjectUtil.isNotNull(spuDTO.getPage()) && ObjectUtil.isNotNull(spuDTO.getRows())){
            PageHelper.startPage(spuDTO.getPage(),spuDTO.getRows());
        }

        Example example = new Example(SpuEntity.class);
        Example.Criteria criteria = example.createCriteria();

        //判断前台传来的
        if (ObjectUtil.isNotNull(spuEntity.getSaleable()) && spuEntity.getSaleable() != 2){
            criteria.andEqualTo("saleable",spuEntity.getSaleable());
        }
        if (ObjectUtil.isNotNull(spuEntity.getTitle())){
            criteria.andLike("title","%"+spuEntity.getTitle()+"%");
        }

        List<SpuEntity> list = spuMapper.selectByExample(example);
        PageInfo<SpuEntity> pageInfo = new PageInfo<>(list);
        return this.setResultSuccess(pageInfo);
    }
}