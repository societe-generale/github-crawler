package com.societegenerale.githubcrawler.parsers

import com.societegenerale.githubcrawler.model.SearchResult
import com.societegenerale.githubcrawler.model.SearchResultItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class ItemPathSearchResultParserTest{

    private val parser = ItemPathSearchResultParser()

    @Test
    fun shouldYieldListOfPathsFound() {

        val searchResults=listOf(SearchResultItem("path1"),SearchResultItem("path2"))

        val searchResultWith2Hits= SearchResult(searchResults.size,searchResults)

        val parsingResult=parser.parse(searchResultWith2Hits)

        assertThat(parsingResult).isInstanceOf(List::class.java)

        if(parsingResult is List<*>){
            assertThat(parsingResult).containsExactly("path1","path2")
        }
    }


    @Test
    fun shouldYield_NotFound_WhenNoMatch() {

        val nothingFoundSearchResult= SearchResult(0, emptyList())

        assertThat(parser.parse(nothingFoundSearchResult)).isEqualTo("not found")
    }
}