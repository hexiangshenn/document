package com.baidu.shop.service.impl;

import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.component.MrRabbitMQ;
import com.baidu.shop.constant.MqMessageConstant;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.dto.SpuDetailDTO;
import com.baidu.shop.entity.*;
import com.baidu.shop.mapper.*;
import com.baidu.shop.service.GoodsService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Resource
    private SpuDetailMapper spuDetailMapper;
    @Resource
    private SkuMapper skuMapper;
    @Resource
    private StockMapper stockMapper;
    @Autowired
    private MrRabbitMQ mrRabbitMQ;

    //提取重复的 删除sku stock 代码
    private void deleteSkuAndStock (Integer spuId){
        System.out.println("spuId="+spuId);
        Example example = new Example(SkuEntity.class);
        example.createCriteria().andEqualTo("spuId",spuId);
        List<SkuEntity> skuEntities = skuMapper.selectByExample(example);
        System.out.println("skuEntities="+skuEntities);
        //得到skuId集合
        List<Long> skuIdList = skuEntities.stream().map(skuEntity -> skuEntity.getId()).collect(Collectors.toList());
        System.out.println("skuIdList="+skuIdList);
        skuMapper.deleteByIdList(skuIdList);//通过skuId集合删除sku信息
        stockMapper.deleteByIdList(skuIdList);//通过skuId集合删除stock信息
    }
    //提取重复的 新增sku stock 代码
    private void insertSkuAndStock (SpuDTO spuDTO,Integer spuId,Date date){
        List<SkuDTO> skus = spuDTO.getSkus();
        skus.stream().forEach(skuDTO -> {

            SkuEntity skuEntity = BaiduBeanUtil.copyProperties(skuDTO, SkuEntity.class);
            skuEntity.setSpuId(spuId);
            skuEntity.setCreateTime(date);
            skuEntity.setLastUpdateTime(date);
            skuMapper.insertSelective(skuEntity);

            //新增stock
            StockEntity stockEntity = new StockEntity();
            stockEntity.setSkuId(skuEntity.getId());
            stockEntity.setStock(skuDTO.getStock());
            stockMapper.insertSelective(stockEntity);
        });
    }


    @Transactional
    @Override//删除
    public Result<JsonObject> deleteGoods(Integer spuId) {
        spuMapper.deleteByPrimaryKey(spuId);
        spuDetailMapper.deleteByPrimaryKey(spuId);

        this.deleteSkuAndStock(spuId);

        mrRabbitMQ.send(spuId + "",MqMessageConstant.SPU_ROUT_KEY_DELETE);
        return setResultSuccess();
    }

    //修改
    @Override
    @Transactional
    public Result<JsonObject> editGoods(SpuDTO spuDTO) {
        final Date date = new Date();
        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setLastUpdateTime(date);
        spuMapper.updateByPrimaryKeySelective(spuEntity);

        spuDetailMapper.updateByPrimaryKeySelective(BaiduBeanUtil.copyProperties(spuDTO.getSpuDetail(),SpuDetailEntity.class));

        //删除
        Example example = new Example(SkuEntity.class);
        example.createCriteria().andEqualTo("spuId",spuEntity.getId());
        List<SkuEntity> skuEntities = skuMapper.selectByExample(example);

        this.deleteSkuAndStock(spuEntity.getId());
        this.insertSkuAndStock(spuDTO,spuEntity.getId(),date);

        mrRabbitMQ.send(spuDTO.getId() + "",MqMessageConstant.SPU_ROUT_KEY_UPDATE);
        return this.setResultSuccess();
    }

    @Override
    public Result<SpuDetailEntity> getSpuDetailBySpu(Integer spuId) {
        SpuDetailEntity spuDetailEntity = spuDetailMapper.selectByPrimaryKey(spuId);
        return this.setResultSuccess(spuDetailEntity);
    }

    @Override
    public Result<List<SkuDTO>> getSkuBySpuId(Integer spuId) {
        List<SkuDTO> list = skuMapper.selectSkuAndStockBySpuId(spuId);

        return this.setResultSuccess(list);
    }


    //新增!!!!!!!
    @Override
    public Result<JsonObject> saveGoods(SpuDTO spuDTO) {
        Integer spuId = this.saveGoodsTransactional(spuDTO);
        mrRabbitMQ.send(spuId + "", MqMessageConstant.SPU_ROUT_KEY_SAVE);
        return this.setResultSuccess();
    }

    @Transactional
    public Integer saveGoodsTransactional(SpuDTO spuDTO){
        final Date date = new Date();
        //新增spu
        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setSaleable(1);
        spuEntity.setValid(1);
        spuEntity.setCreateTime(date);
        spuEntity.setLastUpdateTime(date);
        spuMapper.insertSelective(spuEntity);

        //新增大字段
        SpuDetailDTO spuDetail = spuDTO.getSpuDetail();
        SpuDetailEntity spuDetailEntity = BaiduBeanUtil.copyProperties(spuDetail, SpuDetailEntity.class);
        spuDetailEntity.setSpuId(spuEntity.getId());
        spuDetailMapper.insertSelective(spuDetailEntity);
        //新增sku,sku可能时多条数据
        this.insertSkuAndStock(spuDTO,spuEntity.getId(),date);
        return spuEntity.getId();
    }

    @Override
    public Result<List<SpuDTO>> getSpuInfo(SpuDTO spuDTO) {

        if (ObjectUtil.isNotNull(spuDTO.getPage()) && ObjectUtil.isNotNull(spuDTO.getRows())){
            PageHelper.startPage(spuDTO.getPage(),spuDTO.getRows());
        }
        if(!StringUtils.isEmpty(spuDTO.getSort()) && !StringUtils.isEmpty(spuDTO.getOrder())){
            PageHelper.orderBy(spuDTO.getOrderBy());
        }
        Example example = new Example(SpuEntity.class);
        Example.Criteria criteria = example.createCriteria();

        if (ObjectUtil.isNotNull(spuDTO.getSaleable()) && spuDTO.getSaleable() != 2){
            criteria.andEqualTo("saleable",spuDTO.getSaleable());
        }
        if (!StringUtils.isEmpty(spuDTO.getTitle())){
            criteria.andEqualTo("title","%"+spuDTO.getTitle()+"%");
        }
        if(ObjectUtil.isNotNull(spuDTO.getId()))
            criteria.andEqualTo("id",spuDTO.getId());

        List<SpuEntity> spuEntities = spuMapper.selectByExample(example);

        List<SpuDTO> spuDTOList = spuEntities.stream().map(spuEntity -> {
            SpuDTO spuDTO1 = BaiduBeanUtil.copyProperties(spuEntity,SpuDTO.class);

            List<CategoryEntity> categoryEntities = categoryMapper.selectByIdList(Arrays.asList(spuEntity.getCid1(), spuEntity.getCid2(), spuEntity.getCid3()));

            String categoryName = categoryEntities.stream().map(categoryEntity -> categoryEntity.getName()).collect(Collectors.joining("/"));
            spuDTO1.setCategoryName(categoryName);

            BrandEntity brandEntity = brandMapper.selectByPrimaryKey(spuEntity.getBrandId());
            spuDTO1.setBrandName(brandEntity.getName());
            return spuDTO1;
        }).collect(Collectors.toList());

        PageInfo<SpuEntity> pageInfo = new PageInfo<>(spuEntities);
        return this.setResult(HTTPStatus.OK,pageInfo.getTotal() + "",spuDTOList);
    }

    //上架下架
    @Transactional
    @Override
    public Result<JsonObject> upAndDown(SpuDTO spuDTO) {
        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setId(spuDTO.getId());
        spuEntity.setSaleable(spuDTO.getSaleable());
        spuMapper.updateByPrimaryKeySelective(spuEntity);
        return this.setResultSuccess();
    }
}