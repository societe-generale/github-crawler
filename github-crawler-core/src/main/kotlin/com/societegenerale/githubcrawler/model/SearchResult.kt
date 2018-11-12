package com.societegenerale.githubcrawler.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SearchResult (
        @JsonProperty("total_count")
        val totalCount: Int,
        @JsonProperty("items")
        val items : List<SearchResultItem>)



class SearchResultItem (val path:String)
