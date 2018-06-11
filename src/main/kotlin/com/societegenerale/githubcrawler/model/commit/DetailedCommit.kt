package com.societegenerale.githubcrawler.model.commit

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.societegenerale.githubcrawler.model.Author


@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailedCommit(val sha: String,
                          val author: Author?,
                          val stats: CommitStats)
