package com.societegenerale.githubcrawler.model.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SelfWithClone (
    val clone: List<Clone>,
    val self: List<Self>
    )
