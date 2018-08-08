package com.societegenerale.githubcrawler

import com.google.common.collect.ImmutableList
import com.societegenerale.githubcrawler.remote.NoReachableRepositories
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value


class ConfigValidator(val properties : GitHubCrawlerProperties,
                      @Value("\${gitHub.url}")
                      val gitHubUrl : String = "",
                      @Value("\${organizationName}")
                      private val organizationName: String = "",
                      private val remoteGitHub: RemoteGitHub) {

    val log = LoggerFactory.getLogger(this.javaClass)

    fun getValidationErrors(): ImmutableList<String> {

        val validationErrors = mutableListOf<String>()

        if(gitHubUrl.isEmpty()){
            validationErrors.add("gitHub.url can't be empty")
        }

        if(organizationName.isEmpty()){
            validationErrors.add("organization can't be empty")
        }

        try{
            remoteGitHub.validateRemoteConfig(organizationName)
        }
        catch(e : NoReachableRepositories){
            val errorMessage = "Not able to fetch repositories from the organization ${organizationName} on URL ${gitHubUrl}. This could be due to several things :\n"+
                    "\t\t - URL should be the API URL. For github.com it's https://api.github.com, for Github Enterprise, it's usually https://myGHEserver/api/v3 (no trailing slash) \n"+
                    "\t\t - the organization doesn't exist\n"+
                    "\t\t - remote server is not reachable (are you using a proxy ?)\n\n"+
                    "See detailed stacktrace for further investigation if required"

            log.error(errorMessage,e)
            validationErrors.add(errorMessage)
        }

        return ImmutableList.copyOf(validationErrors);

    }

}
