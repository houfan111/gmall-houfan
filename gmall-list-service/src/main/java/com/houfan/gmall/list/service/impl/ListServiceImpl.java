package com.houfan.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.houfan.gmall.bean.SkuLsInfo;
import com.houfan.gmall.bean.SkuLsParam;
import com.houfan.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;


    @Override
    public List<SkuLsInfo> searchBySkuLsParam(SkuLsParam skuLsParam) {
            // 调用方法得到es查询语句
            String query = makeQueryStringForSearch(skuLsParam);
            // 是内部类,addIndex表示添加搜索实例的名字,addType表示表名,得到一个搜索对象
            Search search = new Search.Builder(query).addIndex("gmall").addType("SkuLsInfo").build();

            // 执行查询
            SearchResult searchResult = null;
            try {
                searchResult = jestClient.execute(search);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 从里面得到hits
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        List<SkuLsInfo> skuLsInfos = new ArrayList<>();
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            // 代表每一个查出来的数据对象
            SkuLsInfo skuLsInfo = hit.source;

            // 判断高亮不为null(有可能是从三级分类那儿进来,没有keyword)
            if (hit.highlight != null){
                //  获取高亮的名字对应的值
                List<String> skuNameHI = hit.highlight.get("skuName");
                // 只有一个,但是用集合封装的,不知道为啥
                String skuName = skuNameHI.get(0);
                skuLsInfo.setSkuName(skuName);
            }
                skuLsInfos.add(skuLsInfo);
        }

        return skuLsInfos;
    }


    private String makeQueryStringForSearch(SkuLsParam skuLsParam){

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // boot
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        // must查询
        String keyword = skuLsParam.getKeyword();
        if(StringUtils.isNoneBlank(keyword)){
            // 说明是从检索页面过来的
            // 创建一个query对象,根据skuName进行检索es中的keyword
            MatchQueryBuilder queryBuilder = new MatchQueryBuilder("skuName", keyword);
            // 加入到must中
            boolQueryBuilder.must(queryBuilder);

            // highlight(使用标签来替换查询出来的数据的文字信息,高亮显示)
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            // 设置前缀
            highlightBuilder.preTags("<span style='color:red;font-weight:bolder;'>");
            // 这是需要高亮的key
            highlightBuilder.field("skuName");
            // 设置后缀
            highlightBuilder.postTags("</span>");
            // 将高亮加入查询源
            searchSourceBuilder.highlight(highlightBuilder);
        }

        // 判断三级分类id不为null的情况
        Integer catalog3Id = skuLsParam.getCatalog3Id();
        if(catalog3Id != null){
            // 说明从三级分类处过来的,显示三级分类下的前二十个
            MatchQueryBuilder queryBuilder = new MatchQueryBuilder("catalog3Id", catalog3Id);
            // 加入到must中
            boolQueryBuilder.must(queryBuilder);
        }

        Integer[] valueIds = skuLsParam.getValueId();
        if (valueIds != null && valueIds.length != 0){
            for (Integer valueId : valueIds) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        // 组合成查询语句
        searchSourceBuilder.query(boolQueryBuilder);

        // 设置搜索显示的页面数,也就是可以用来制作分页
        searchSourceBuilder.size(20);
        searchSourceBuilder.from(0);

        return searchSourceBuilder.toString();
    }

}
