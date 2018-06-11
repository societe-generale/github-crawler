package com.societegenerale.githubcrawler.model.team

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TeamMember(val id: String,
                      val login: String)