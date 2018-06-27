package com.societegenerale.githubcrawler.model.team

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Team(val id: String,
                val name: String)