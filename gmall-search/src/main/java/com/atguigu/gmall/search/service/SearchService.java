package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.entity.SearchParamVo;
import com.atguigu.gmall.search.entity.SearchResponseVo;

import java.io.IOException;

public interface SearchService {

    SearchResponseVo search(SearchParamVo searchParamVo) throws IOException;
}
