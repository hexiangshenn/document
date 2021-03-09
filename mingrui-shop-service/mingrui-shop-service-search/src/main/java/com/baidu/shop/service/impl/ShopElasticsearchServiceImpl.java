package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.document.GoodsDoc;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpecParamDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.entity.CategoryEntity;
import com.baidu.shop.entity.SpecParamEntity;
import com.baidu.shop.entity.SpuDetailEntity;
import com.baidu.shop.feign.BrandFeign;
import com.baidu.shop.feign.CategoryFeign;
import com.baidu.shop.feign.GoodsFeign;
import com.baidu.shop.feign.SpecificationFeign;
import com.baidu.shop.response.GoodsResponse;
import com.baidu.shop.service.ShopElasticsearchService;
import com.baidu.shop.service.SpecificationService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.HighlightUtil;
import com.baidu.shop.utils.JSONUtil;
import com.google.gson.JsonObject;
import com.netflix.discovery.converters.Auto;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName ShopElasticsearchServiceImpl
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/4
 * @Version V1.0
 **/
@RestController
public class ShopElasticsearchServiceImpl extends BaseApiService implements ShopElasticsearchService{

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

    @Override
    public Result<List<GoodsDoc>> search(String search,Integer page,String filter) {

        NativeSearchQueryBuilder nativeSearchQueryBuilder = this.getNativeSearchQueryBuilder(search, page, filter);
        SearchHits<GoodsDoc> searchHits = elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), GoodsDoc.class);

        List<GoodsDoc> goodsDocs = HighlightUtil.getHighlightList(searchHits.getSearchHits());

        long total = searchHits.getTotalHits();
        long totalPage = Double.valueOf(Math.ceil(Double.valueOf(total) / 10)).longValue();

        Map<Integer, List<CategoryEntity>> map = this.getCategoryListByBucket(searchHits.getAggregations());
        Integer hotCid = 0;
        List<CategoryEntity> categoryList = null;
        for (Map.Entry<Integer, List<CategoryEntity>> entry : map.entrySet()){
            hotCid = entry.getKey();
            categoryList = entry.getValue();
        }

        return new GoodsResponse(total,totalPage,categoryList
                ,this.getBrandListByBucket(searchHits.getAggregations()),goodsDocs
                ,this.getSpecMap(hotCid, search));
    }

    private Map<String, List<String>> getSpecMap(Integer hotCid,String search){
        SpecParamDTO specParamDTO = new SpecParamDTO();
        specParamDTO.setCid(hotCid);
        specParamDTO.setSearching(true);
        Result<List<SpecParamEntity>> specParamInfo = specificationFeign.getSpecParamInfo(specParamDTO);
        Map<String, List<String>> specMap = new HashMap<>();
        if (specParamInfo.isSuccess()) {

            List<SpecParamEntity> specParamList = specParamInfo.getData();

            NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
            nativeSearchQueryBuilder.withQuery(
                    QueryBuilders.multiMatchQuery(search,"title","brandName","categoryName")
            );
            nativeSearchQueryBuilder.withPageable(PageRequest.of(0,1));
            specParamList.stream().forEach(specParam -> {
                nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms(specParam.getName())
                        .field("specs." + specParam.getName() + ".keyword"));
            });

            SearchHits<GoodsDoc> searchHits = elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), GoodsDoc.class);
            Aggregations aggregations = searchHits.getAggregations();

            specParamList.stream().forEach(specParam -> {

                Terms aggregation = aggregations.get(specParam.getName());
                List<? extends Terms.Bucket> buckets = aggregation.getBuckets();
                List<String> valueList = buckets.stream().map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());

                specMap.put(specParam.getName(),valueList);
            });
        }

        return specMap;
    }

    private NativeSearchQueryBuilder getNativeSearchQueryBuilder(String search, Integer page,String filter){
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        //多字段查询
        nativeSearchQueryBuilder.withQuery(
                QueryBuilders.multiMatchQuery(search,"title","brandName","categoryName")
        );

        //过滤查询
        if (!StringUtils.isEmpty(filter) && filter.length() > 2){
            //将字符串转换成map集合
            Map<String, String> filterMap = JSONUtil.toMapValueString(filter);
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            //遍历map
            filterMap.forEach((key,value) -> {
                MatchQueryBuilder matchQueryBuilder = null;
                //判断key是否为cid3喝brandId
                if (key.equals("brandId") || key.equals("cid3")){
                    matchQueryBuilder = QueryBuilders.matchQuery(key, value);
                }else {
                    matchQueryBuilder = QueryBuilders.matchQuery("specs." + key + ".keyword",value);
                }
                boolQueryBuilder.must(matchQueryBuilder);
            });
            nativeSearchQueryBuilder.withFilter(boolQueryBuilder);
        }

        //结果过滤
        nativeSearchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","title","skus"},null));
        //分页
        nativeSearchQueryBuilder.withPageable(PageRequest.of(page -1,10));
        //高亮
        nativeSearchQueryBuilder.withHighlightBuilder(HighlightUtil.getHighlightBuilder("title"));
        //聚合 -->> 品牌    分类聚合
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("agg_category").field("cid3"));
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("agg_brand").field("brandId"));
        return nativeSearchQueryBuilder;
    }

    //通过聚合得到分类List
    private Map<Integer, List<CategoryEntity>> getCategoryListByBucket(Aggregations aggregations ){

        Terms agg_category = aggregations.get("agg_category");
        List<? extends Terms.Bucket> categoryBuckets = agg_category.getBuckets();
        List<Long> docCount = Arrays.asList(0L);
        List<Integer> hotCid = Arrays.asList(0);
        List<String> categoryIdList = categoryBuckets.stream().map(categoryBucket -> {

            if(categoryBucket.getDocCount() > docCount.get(0)){

                docCount.set(0,categoryBucket.getDocCount());
                hotCid.set(0,categoryBucket.getKeyAsNumber().intValue());
            }

            return categoryBucket.getKeyAsNumber().longValue() + "";
        }).collect(Collectors.toList());

        Result<List<CategoryEntity>> categoryResult = categoryFeign.getCateByIds(String.join(",", categoryIdList));

        List<CategoryEntity> categoryList = null;
        if(categoryResult.isSuccess()){
            categoryList = categoryResult.getData();
        }

        Map<Integer, List<CategoryEntity>> map = new HashMap<>();
        map.put(hotCid.get(0),categoryList);

        return map;
    }

    private List<BrandEntity> getBrandListByBucket(Aggregations aggregations){
        Terms agg_brand = aggregations.get("agg_brand");
        List<? extends Terms.Bucket> brandBuckets = agg_brand.getBuckets();
        List<String> brandIdList = brandBuckets.stream().map(brandBucket -> brandBucket.getKeyAsNumber().longValue() + "").collect(Collectors.toList());
        Result<List<BrandEntity>> brandResult = brandFeign.getBrandByIds(String.join(",", brandIdList));
        List<BrandEntity> brandList = null;
        if(brandResult.isSuccess()){
            brandList = brandResult.getData();
        }
        return brandList;
    }


    @Override
    public Result<JSONObject> clearGoodsEsData() {
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(GoodsDoc.class);
        if (indexOperations.exists()){
            indexOperations.delete();
        }
        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> initGoodsEsData() {

        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(GoodsDoc.class);
        if (!indexOperations.exists()){
            indexOperations.create();
            indexOperations.createMapping();
        }

        List<GoodsDoc> goodsDocs = this.esGoodsInfo();
        elasticsearchRestTemplate.save(goodsDocs);
        return this.setResultSuccess();
    }

    //@Override
    private List<GoodsDoc> esGoodsInfo() {
        SpuDTO spuDTO = new SpuDTO();
        /*spuDTO.setPage(1);
        spuDTO.setRows(5);*/

        Result<List<SpuDTO>> spuInfo = goodsFeign.getSpuInfo(spuDTO);
        if(spuInfo.isSuccess()){
            List<SpuDTO> spuList = spuInfo.getData();
            List<GoodsDoc> goodsDocList = spuList.stream().map(spu -> {
                //spu
                GoodsDoc goodsDoc = new GoodsDoc();
                goodsDoc.setId(spu.getId().longValue());
                goodsDoc.setTitle(spu.getTitle());
                goodsDoc.setBrandName(spu.getBrandName());
                goodsDoc.setCategoryName(spu.getCategoryName());
                goodsDoc.setSubTitle(spu.getSubTitle());
                goodsDoc.setBrandId(spu.getBrandId().longValue());
                goodsDoc.setCid1(spu.getCid1().longValue());
                goodsDoc.setCid2(spu.getCid2().longValue());
                goodsDoc.setCid3(spu.getCid3().longValue());
                goodsDoc.setCreateTime(spu.getCreateTime());
                //sku数据 , 通过spuid查询skus
                Map<List<Long>, List<Map<String, Object>>> skusAndPriceMap = this.getSkusAndPriceList(spu.getId());
                skusAndPriceMap.forEach((key,value) -> {
                    goodsDoc.setPrice(key);
                    goodsDoc.setSkus(JSONUtil.toJsonString(value));
                });
                //设置规格参数
                Map<String, Object> specMap = this.getSpecMap(spu);
                goodsDoc.setSpecs(specMap);
                return goodsDoc;
            }).collect(Collectors.toList());

            return goodsDocList;
        }
        return null;
    }

    //获取规格参数map
    private Map<String, Object> getSpecMap(SpuDTO spu){

        SpecParamDTO specParamDTO = new SpecParamDTO();
        specParamDTO.setCid(spu.getCid3());
        specParamDTO.setSearching(true);
        Result<List<SpecParamEntity>> specParamInfo = specificationFeign.getSpecParamInfo(specParamDTO);
        if(specParamInfo.isSuccess()){

            List<SpecParamEntity> specParamList = specParamInfo.getData();
            Result<SpuDetailEntity> spuDetailInfo = goodsFeign.getSpuDetailBySpu(spu.getId());
            if(spuDetailInfo.isSuccess()){

                SpuDetailEntity spuDetailEntity = spuDetailInfo.getData();
                Map<String, Object> specMap = this.getSpecMap(specParamList, spuDetailEntity);
                return specMap;
            }
        }

        return null;
    }

    private Map<String,Object> getSpecMap(List<SpecParamEntity> specParamList ,SpuDetailEntity spuDetailEntity){

        Map<String, Object> specMap = new HashMap<>();
        //将json字符串转换成map集合
        Map<String, String> genericSpec = JSONUtil.toMapValueString(spuDetailEntity.getGenericSpec());
        Map<String, List<String>> specialSpec = JSONUtil.toMapValueStrList(spuDetailEntity.getSpecialSpec());

        //需要查询两张表的数据 spec_param(规格参数名) spu_detail(规格参数值) --> 规格参数名 : 规格参数值
        specParamList.stream().forEach(specParam -> {

            if (specParam.getGeneric()) {//判断从那个map集合中获取数据
                if(specParam.getNumeric() && !StringUtils.isEmpty(specParam.getSegments())){

                    specMap.put(specParam.getName()
                            , chooseSegment(genericSpec.get(specParam.getId() + ""), specParam.getSegments(), specParam.getUnit()));
                }else{

                    specMap.put(specParam.getName(),genericSpec.get(specParam.getId() + ""));
                }

            }else{

                specMap.put(specParam.getName(),specialSpec.get(specParam.getId() + ""));
            }

        });

        return specMap;
    }

    private Map<List<Long>, List<Map<String, Object>>> getSkusAndPriceList(Integer spuId){

        Map<List<Long>, List<Map<String, Object>>> hashMap = new HashMap<>();
        Result<List<SkuDTO>> skusInfo = goodsFeign.getSkuBySpuId(spuId);
        if (skusInfo.isSuccess()) {
            List<SkuDTO> skuList = skusInfo.getData();
            List<Long> priceList = new ArrayList<>();//一个spu的所有商品价格集合

            List<Map<String, Object>> skuMapList = skuList.stream().map(sku -> {

                Map<String, Object> map = new HashMap<>();
                map.put("id", sku.getId());
                map.put("title", sku.getTitle());
                map.put("image", sku.getImages());
                map.put("price", sku.getPrice());

                priceList.add(sku.getPrice().longValue());
                //id ,title ,image,price
                return map;
            }).collect(Collectors.toList());

            hashMap.put(priceList,skuMapList);
            /*goodsDoc.setPrice(priceList);
            goodsDoc.setSkus(JSONUtil.toJsonString(skuMapList));*/
        }
        return hashMap;
    }

    private String chooseSegment(String value, String segments, String unit) {//800 -> 5000-1000
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : segments.split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + unit + "以上";
                }else if(begin == 0){
                    result = segs[1] + unit + "以下";
                }else{
                    result = segment + unit;
                }
                break;
            }
        }
        return result;
    }
}
