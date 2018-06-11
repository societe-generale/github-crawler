package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.model.SearchResult


interface SearchResultParser {

    fun getNameInConfig(): String

    fun parse(searchResult : SearchResult): String

}