package com.societegenerale.githubcrawler.model.bitbucket

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Repository(

    val defaultBranch: String = "master",

    @JsonProperty("slug")
    val fullName: String,

    val name: String,

    val forkable: Boolean,

    val links: SelfWithClone,

    val id: Int

)