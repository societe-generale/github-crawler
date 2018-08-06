package com.societegenerale.githubcrawler

import com.google.common.collect.ImmutableList
import org.springframework.beans.factory.annotation.Value

class ConfigValidator(val properties : GitHubCrawlerProperties,
                      @Value("\${gitHub.url}")
                      val gitHubUrl : String = "",
                      @Value("\${organizationName}")
                      private val organizationName: String = "") {


    fun getValidationErrors(): ImmutableList<String> {

        val validationErrors = mutableListOf<String>()

        if(gitHubUrl.isEmpty()){
            validationErrors.add("gitHub.url can't be empty")
        }

        if(organizationName.isEmpty()){
            validationErrors.add("organization can't be empty")
        }




        return ImmutableList.copyOf(validationErrors);

    }

}
