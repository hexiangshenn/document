package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.*;
import com.baidu.shop.entity.*;
import com.baidu.shop.feign.BrandFeign;
import com.baidu.shop.feign.CategoryFeign;
import com.baidu.shop.feign.GoodsFeign;
import com.baidu.shop.feign.SpecificationFeign;
import com.baidu.shop.service.TemplateService;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName TemplateServiceImpl
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/9
 * @Version V1.0
 **/
@RestController
public class TemplateServiceImpl extends BaseApiService implements TemplateService {

    private final Integer CREATE_STATIC_HTML = 1;
    private final Integer DELETE_STATIC_HTML = 2;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private GoodsFeign goodsFeign;

    @Autowired
    private SpecificationFeign specificationFeign;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private BrandFeign brandFeign;

    @Autowired
    private CategoryFeign categoryFeign;

    @Value(value = "${mrshop.static.html.path}")
    private String htmlPath;

    @Override
    public Result<JSONObject> delHTMLBySpuId(Integer spuId) {
        File file = new File(htmlPath + File.separator + spuId + ".html");
        if(!file.delete()){
            return this.setResultError("文件删除失败");
        }
        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> createStaticHTMLTemplate(Integer spuId) {

        //得到要渲染的数据
        Map<String, Object> goodsInfo = this.getGoodsInfo(spuId);

        Context context = new Context();
        context.setVariables(goodsInfo);

        File file = new File(htmlPath, spuId + ".html");
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file, "UTF-8");
            templateEngine.process("item",context,writer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }finally {
            if (ObjectUtil.isNotNull(writer)) {
                writer.close();
            }
        }

        return this.setResultSuccess();
    }

    private Map<String, Object> getGoodsInfo(Integer spuId) {

        Map<String, Object> goodsInfoMap = new HashMap<>();
        //spu
        SpuDTO spuResultData = this.getSpuInfo(spuId);
        goodsInfoMap.put("spuInfo",spuResultData);
        //spuDetail
        goodsInfoMap.put("spuDetail",this.getSpuDetail(spuId));
        //分类信息
        goodsInfoMap.put("categoryInfo",this.getCategoryInfo(spuResultData.getCid1() + ""
                ,spuResultData.getCid2() + "",spuResultData.getCid3() + ""));
        //品牌信息
        goodsInfoMap.put("brandInfo",this.getBrandInfo(spuResultData.getBrandId()));
        //sku
        goodsInfoMap.put("skus",this.getSkus(spuId));
        //规格组,规格参数(通用)
        goodsInfoMap.put("specGroupAndParam",this.getSpecGroupAndParam(spuResultData.getCid3()));
        //特殊规格
        goodsInfoMap.put("specParamMap",this.getSpecParamMap(spuResultData.getCid3()));

        return goodsInfoMap;
    }

    private SpuDTO getSpuInfo(Integer spuId){
        SpuDTO spuDTO = new SpuDTO();
        spuDTO.setId(spuId);
        Result<List<SpuDTO>> spuResult = goodsFeign.getSpuInfo(spuDTO);
        SpuDTO spuResultData = null;
        if(spuResult.isSuccess()){
            spuResultData = spuResult.getData().get(0);
        }
        return spuResultData;
    }

    private SpuDetailEntity getSpuDetail(Integer spuId){

        SpuDetailEntity spuDetailEntity = null;
        Result<SpuDetailEntity> spuDetailResult = goodsFeign.getSpuDetailBySpu(spuId);
        if(spuDetailResult.isSuccess()){
            spuDetailEntity = spuDetailResult.getData();
        }
        return spuDetailEntity;
    }

    private List<CategoryEntity> getCategoryInfo(String cid1,String cid2,String cid3){

        List<CategoryEntity> categoryEntityList = null;

        Result<List<CategoryEntity>> categoryResult = categoryFeign.getCateByIds(
                String.join(
                        ","
                        , Arrays.asList(cid1, cid2, cid3)
                )
        );
        if(categoryResult.isSuccess()){
            categoryEntityList = categoryResult.getData();
        }
        return categoryEntityList;
    }

    private BrandEntity getBrandInfo(Integer brandId){

        BrandEntity brandEntity = null;

        BrandDTO brandDTO = new BrandDTO();
        brandDTO.setId(brandId);
        Result<PageInfo<BrandEntity>> brandResult = brandFeign.getBrandInfo(brandDTO);
        if(brandResult.isSuccess()){
            brandEntity = brandResult.getData().getList().get(0);
        }

        return brandEntity;
    }

    private List<SkuDTO> getSkus(Integer spuId){

        List<SkuDTO> skuList = null;

        Result<List<SkuDTO>> skusResult = goodsFeign.getSkuBySpuId(spuId);
        if(skusResult.isSuccess()){
            skuList = skusResult.getData();
        }
        return skuList;
    }

    private List<SpecGroupDTO> getSpecGroupAndParam(Integer cid3){

        List<SpecGroupDTO> specGroupAndParam = null;

        SpecGroupDTO specGroupDTO = new SpecGroupDTO();
        specGroupDTO.setCid(cid3);
        Result<List<SpecGroupEntity>> specGroupResult = specificationFeign.getSpecGroupInfo(specGroupDTO);
        if(specGroupResult.isSuccess()){

            List<SpecGroupEntity> specGroupList = specGroupResult.getData();
            specGroupAndParam = specGroupList.stream().map(specGroup -> {
                SpecGroupDTO specGroupDTO1 = BaiduBeanUtil.copyProperties(specGroup, SpecGroupDTO.class);

                SpecParamDTO specParamDTO = new SpecParamDTO();
                specParamDTO.setGroupId(specGroupDTO1.getId());
                specParamDTO.setGeneric(true);
                Result<List<SpecParamEntity>> specParamResult = specificationFeign.getSpecParamInfo(specParamDTO);
                if (specParamResult.isSuccess()) {
                    specGroupDTO1.setSpecList(specParamResult.getData());
                }
                return specGroupDTO1;
            }).collect(Collectors.toList());
        }
        return specGroupAndParam;
    }

    private Map<Integer, String> getSpecParamMap(Integer cid3){

        Map<Integer, String> specParamMap = new HashMap<>();

        SpecParamDTO specParamDTO = new SpecParamDTO();
        specParamDTO.setCid(cid3);
        specParamDTO.setGeneric(false);
        Result<List<SpecParamEntity>> specParamResult = specificationFeign.getSpecParamInfo(specParamDTO);
        if(specParamResult.isSuccess()){
            List<SpecParamEntity> specParamEntityList = specParamResult.getData();
            specParamEntityList.stream().forEach(specParam -> specParamMap.put(specParam.getId(),specParam.getName()));
        }
        return specParamMap;
    }

    @Override
    public Result<JSONObject> initStaticHTMLTemplate() {

        Result<List<SpuDTO>> spuInfo = goodsFeign.getSpuInfo(new SpuDTO());
        if (spuInfo.isSuccess()){
            spuInfo.getData().stream().forEach(spuDTO -> {
                this.createStaticHTMLTemplate(spuDTO.getId());
            });
        }
        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> clearStaticHTMLTemplate() {

        Result<List<SpuDTO>> spuInfo = goodsFeign.getSpuInfo(new SpuDTO());
        if (spuInfo.isSuccess()){
            spuInfo.getData().stream().forEach(spuDTO -> {
                this.deleteStaticHTMLTemplate(spuDTO.getId());
            });
        }
        return this.setResultSuccess();
    }

    private Boolean operationStaticHTML(Integer operation){

        try {
            Result<List<SpuDTO>> spuInfo = goodsFeign.getSpuInfo(new SpuDTO());
            if (spuInfo.isSuccess()){
                spuInfo.getData().stream().forEach(spuDTO -> {
                    if (operation == 1){
                        this.createStaticHTMLTemplate(spuDTO.getId());
                    }else {
                        this.deleteStaticHTMLTemplate(spuDTO.getId());
                    }
                });
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Result<JSONObject> deleteStaticHTMLTemplate(Integer spuId) {

        File file = new File(htmlPath, spuId + ".html");
        if (file.exists()){
            file.delete();
        }
        return this.setResultSuccess();
    }
}
