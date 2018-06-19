package com.societegenerale.githubcrawler.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class FileOnRepository(val name: String,
                            val content: String,
                            @JsonProperty("download_url")
                            val downloadUrl: String) {
}