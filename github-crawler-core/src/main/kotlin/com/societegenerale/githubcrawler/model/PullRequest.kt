package com.societegenerale.githubcrawler.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PullRequest(val number: Long)