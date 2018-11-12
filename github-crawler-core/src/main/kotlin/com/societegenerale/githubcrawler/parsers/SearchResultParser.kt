package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.model.SearchResult

/**
 * Implementations will parse/analyse the provided [SearchResult] and return an indicator value
 */
interface SearchResultParser {

    /**
     * @return the parser's ID that we need to use in config to refer to it. Typically, a String describing the parser type.
     * At application startup, beans implementing the interface will be instantiated and stored in a Map as a value, with getNameInConfig value as the key.
     */
    fun getNameInConfig(): String

    /**
     * @param searchResult the search result that we got when the search query was performed for this repository
     * @return the "indicator" that was found, following the parsing/analysis of the provided [SearchResult]
     */
    fun parse(searchResult : SearchResult): Any

}