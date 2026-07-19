package com.societegenerale.githubcrawler.model.bitbucket

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Repositories(val values: Set<Repository>,
                        val isLastPage: Boolean,
                        val start: Int = 0,
                        val nextPageStart: Int = 0,
                        )
