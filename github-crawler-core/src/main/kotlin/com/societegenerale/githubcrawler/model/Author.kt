package com.societegenerale.githubcrawler.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Author(val id: String,
                  val login: String)