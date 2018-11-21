package com.societegenerale.githubcrawler


import com.google.common.collect.ImmutableList
import com.societegenerale.githubcrawler.remote.NoReachableRepositories
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.slf4j.LoggerFactory


class ConfigValidator(val properties : GitHubCrawlerProperties,

                      private val remoteGitHub: RemoteGitHub) {

    val log = LoggerFactory.getLogger(this.javaClass)

    fun getValidationErrors(): ImmutableList<String> {

        val validationErrors = mutableListOf<String>()

        if(properties.githubConfig.apiUrl.isBlank()){
            validationErrors.add("gitHub.apiUrl can't be empty")
        }

        if(properties.githubConfig.organizationName.isBlank()){
            validationErrors.add("organization can't be empty")
        }

        if(validationErrors.isNotEmpty()){
            return ImmutableList.copyOf(validationErrors);
        }

        val organizationName=properties.githubConfig.organizationName;

        try{
            remoteGitHub.validateRemoteConfig(organizationName)
        }
        catch(e : NoReachableRepositories){
            val errorMessage = "Not able to fetch repositories from the organization ${organizationName} on URL ${properties.githubConfig.apiUrl}. This could be due to several things :\n"+
                    "\t\t - URL should be the API URL. For github.com it's https://api.github.com, for Github Enterprise, it's usually https://myGHEserver/api/v3 (no trailing slash) \n"+
                    "\t\t - the organization doesn't exist\n"+
                    "\t\t - remote server is not reachable (are you using a proxy ?)\n\n"+
                    "\t\t - are you using a valid OAuth token ?\n\n"+
                    "See detailed stacktrace for further investigation if required"

            log.error(errorMessage,e)

            return ImmutableList.of(errorMessage)
        }

        return ImmutableList.of();

    }

}
