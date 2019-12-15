package com.atguigu.gmall.search.service.impl;

import com.atguigu.gmall.search.entity.Goods;
import com.atguigu.gmall.search.entity.SearchParamVo;
import com.atguigu.gmall.search.entity.SearchResponseAttrVO;
import com.atguigu.gmall.search.entity.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService{

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public SearchResponseVo search(SearchParamVo searchParamVo) throws IOException {
        SearchRequest searchRequest = this.buildDslSearch(searchParamVo);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(response.toString());

        SearchResponseVo responseVo = this.parseDslSearch(response);
        responseVo.setPageNum(searchParamVo.getPageNum());
        responseVo.setPageSize(searchParamVo.getPageSize());
        return responseVo;
    }

    private SearchResponseVo parseDslSearch(SearchResponse response) throws JsonProcessingException {
        SearchResponseVo responseVo = new SearchResponseVo();
        SearchHits hits = response.getHits();
        responseVo.setTotal(hits.totalHits);

        SearchResponseAttrVO brand = new SearchResponseAttrVO();
        brand.setName("品牌");
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<String> brandValues = brandIdAgg.getBuckets().stream().map(brandBucket -> {
            Map<String, String> map = new HashMap<>();
            map.put("id", brandBucket.getKeyAsString());
            Map<String, Aggregation> brandIdSubMap = brandBucket.getAggregations().asMap();
            ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandIdSubMap.get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            map.put("name",  brandName);

            try {
                return OBJECT_MAPPER.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        brand.setValue(brandValues);
        responseVo.setBrand(brand);
        SearchResponseAttrVO category = new SearchResponseAttrVO();
        category.setName("分类");
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<String> categoryValue = categoryIdAgg.getBuckets().stream().map(categoryBucket -> {
            Map<String,String> map = new HashMap<>();
            map.put("id", categoryBucket.getKeyAsString());
            Map<String, Aggregation> categoryIdSubMap = categoryBucket.getAggregations().asMap();
            ParsedStringTerms categoryNameAgg = (ParsedStringTerms)categoryIdSubMap.get("categoryNameAgg");
            String categoryName = categoryNameAgg.getBuckets().get(0).getKeyAsString();
            map.put("name", categoryName);
            try {
                return OBJECT_MAPPER.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        category.setValue(categoryValue);
        responseVo.setCatelog(category);

        SearchHit[] subHits = hits.getHits();
        List<Goods> goodsList = new ArrayList<>();
        for (SearchHit subHit : subHits) {
            Goods goods = OBJECT_MAPPER.readValue(subHit.getSourceAsString(), new TypeReference<Goods>() {
            });
            goods.setTitle(subHit.getHighlightFields().get("title").getFragments()[0].toString());
            goodsList.add(goods);
        }
        responseVo.setProducts(goodsList);

        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        List<Terms.Bucket> attrBuckets = (List<Terms.Bucket>)attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrBuckets)){
            List<SearchResponseAttrVO> attrList = attrBuckets.stream().map(attrBucket -> {
                SearchResponseAttrVO attrVO = new SearchResponseAttrVO();
                attrVO.setProductAttributeId(attrBucket.getKeyAsNumber().longValue());
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) attrBucket.getAggregations().get("attrNameAgg");
                String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
                attrVO.setName(attrName);
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) attrBucket.getAggregations().get("attrValueAgg");
                List<Terms.Bucket> valueAggBuckets = (List<Terms.Bucket>)attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(valueAggBuckets)){
                    List<String> attrValues = valueAggBuckets.stream().map(valueAggBucket ->
                            valueAggBucket.getKeyAsString()).collect(Collectors.toList());
                    attrVO.setValue(attrValues);
                }
                return attrVO;
            }).collect(Collectors.toList());
            responseVo.setAttrs(attrList);
        }
        return responseVo;
    }

    public SearchRequest buildDslSearch(SearchParamVo searchParamVo){
        String keyword = searchParamVo.getKeyword();
        if (StringUtils.isEmpty(keyword)){
            return null;
        }
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));

        String[] brand = searchParamVo.getBrand();
        if (brand !=null && brand.length !=0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brand));
        }
        String[] catelog3 = searchParamVo.getCatelog3();
        if (catelog3 !=null && catelog3.length !=0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",catelog3));
        }

        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
        Integer priceFrom = searchParamVo.getPriceFrom();
        Integer priceTo = searchParamVo.getPriceTo();
        if (priceFrom != null){
            rangeQueryBuilder.gte(priceFrom);
        }
        if (priceTo != null){
            rangeQueryBuilder.lte(priceTo);
        }
        boolQueryBuilder.filter(rangeQueryBuilder);

        String[] props = searchParamVo.getProps();
        if (props !=null && props.length!=0){
            for (String prop : props) {
                String[] split = StringUtils.split(prop, ":");
                if (split ==null && split.length !=2){
                    continue;
                }
                String[] attrValues = StringUtils.split(split[1], "-");
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                subBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));
                boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));
                boolQueryBuilder.filter(boolQuery);
            }
        }
        sourceBuilder.query(boolQueryBuilder);

        Integer pageNum = searchParamVo.getPageNum();
        Integer pageSize = searchParamVo.getPageSize();
        sourceBuilder.from(pageSize*(pageNum-1));
        sourceBuilder.size(pageSize);


        String order = searchParamVo.getOrder();
        if (!StringUtils.isEmpty(order)){
            String[] split = StringUtils.split(order, ":");
            if (split != null && split.length ==2){
                String field = null;
                switch (split[0]){
                    case "0": field = "createTime";break;
                    case "1": field = "sale" ;break;
                    case "2": field = "price";break;
                }
                sourceBuilder.sort(field,split[1].equals("asc")? SortOrder.ASC:SortOrder.DESC);
            }
        }

        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<em>").postTags("</em>"));

        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brandIdAgg").field("brandId").subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"));
        sourceBuilder.aggregation(brandAgg);
        TermsAggregationBuilder categoryAgg = AggregationBuilders.terms("categoryIdAgg").field("categoryId").subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"));
        sourceBuilder.aggregation(categoryAgg);
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attrAgg", "attrs").
                subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")));
        sourceBuilder.aggregation(attrAgg);

//        sourceBuilder.fetchSource(new String[]{"skuId","pic","title","price"},null);

        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }
}
