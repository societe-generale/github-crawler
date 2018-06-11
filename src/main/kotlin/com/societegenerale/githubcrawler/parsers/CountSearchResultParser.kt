package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.model.SearchResult
import org.springframework.stereotype.Component

@Component
class CountSearchResultParser : SearchResultParser{

    override fun getNameInConfig(): String {
        return "count"
    }

    override fun parse(searchResult: SearchResult): String {

        return searchResult.totalCount.toString()


    }
}