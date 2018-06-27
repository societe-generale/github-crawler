package com.societegenerale.githubcrawler.model.commit

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Commit(val sha: String)

