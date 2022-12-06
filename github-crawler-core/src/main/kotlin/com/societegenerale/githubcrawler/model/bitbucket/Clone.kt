package com.societegenerale.githubcrawler.model.bitbucket

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Clone (
    val href: String,
    val name: String
)