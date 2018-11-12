package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.model.SearchResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemPathSearchResultParser : SearchResultParser{

    val log = LoggerFactory.getLogger(this.javaClass)

    override fun getNameInConfig(): String {
        return "path"
    }

    override fun parse(searchResult: SearchResult): Any {

        if(searchResult.totalCount>0){
            return searchResult.items.map { i -> i.path }
        }
        else{
            return "not found"
        }
    }
}