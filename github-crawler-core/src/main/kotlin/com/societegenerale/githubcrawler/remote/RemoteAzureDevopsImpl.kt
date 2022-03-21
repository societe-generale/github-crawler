package com.societegenerale.githubcrawler.remote

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.societegenerale.githubcrawler.RepositoryConfig
import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.PullRequest
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.model.SearchResult
import com.societegenerale.githubcrawler.model.commit.Commit
import com.societegenerale.githubcrawler.model.commit.DetailedCommit
import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.io.IOUtils
import org.azd.utils.AzDClientApi
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.io.IOException
import java.io.StringWriter
import java.util.*
import java.util.stream.Collectors.toSet


class RemoteAzureDevopsImpl @JvmOverloads constructor(val organization: String, val personalAccessToken: String) : RemoteGitHub {

    companion object {

        const val AZURE_DEVOPS_URL= "https://dev.azure.com/"

        const val AZURE_DEVOPS_API_VERSION= "api-version=6.0"

        const val REPO_LEVEL_CONFIG_FILE = ".azureDevopsCrawler"

        const val BASIC_AUTHENTICATION = "Basic"
    }

    private val splitedOrgName=organization.split("#")
    private var azureOrg=splitedOrgName.get(0)
    private var azureProject=splitedOrgName.get(1)

    private val azDevopsClient= AzDClientApi(azureOrg, azureProject, personalAccessToken)

    private val basicAuthentCredentials: String = Credentials.basic("", personalAccessToken)

    private val requestTemplate=okhttp3.Request.Builder()
        .header("Content-Type", "application/json")
        .header("Authorization", basicAuthentCredentials)

    private val httpClient : OkHttpClient

    init {
        val logging= HttpLoggingInterceptor()
        //logging.level = (HttpLoggingInterceptor.Level.HEADERS)

        httpClient= OkHttpClient.Builder().addInterceptor(logging).build()
    }

    override fun fetchRepositories(organizationName: String): Set<Repository> {

        val repositories=azDevopsClient.gitApi.repositories.repositories

        return repositories.stream().map{ repo -> Repository(
            url=repo.url,
            name =repo.name,
            fullName =repo.name,
            defaultBranch = repo.defaultBranch,
            //TODO make the dates nullable in Repository
            creationDate = Date(),
            lastUpdateDate = Date(),
        )}
            .collect(toSet())

    }


    override fun fetchRepoConfig(repositoryFullName: String, defaultBranch: String): RepositoryConfig {

        val configUrl=AZURE_DEVOPS_URL+"${azureOrg}/${azureProject}/_apis/git/repositories/${repositoryFullName}/items?path=${REPO_LEVEL_CONFIG_FILE}&${
            AZURE_DEVOPS_API_VERSION}"

        val request = requestTemplate.url(configUrl).build()

        val response=httpClient.newCall(request).execute()

        val decoder = AzureDevopsResponseDecoder()

        return decoder.decodeRepoConfig(response)
    }

    override fun fetchRepoBranches(repositoryFullName: String): Set<Branch> {
        TODO("Not yet implemented")
    }

    override fun fetchCodeSearchResult(repositoryFullName: String, query: String): SearchResult {
        TODO("Not yet implemented")
    }

    override fun fetchFileContent(repositoryFullName: String, branchName: String, fileToFetch: String): String {
        TODO("Not yet implemented")
    }

    override fun fetchCommits(repositoryFullName: String, perPage: Int): Set<Commit> {
        TODO("Not yet implemented")
    }

    override fun fetchCommit(repositoryFullName: String, commitSha: String): DetailedCommit {
        TODO("Not yet implemented")
    }

    override fun fetchTeams(organizationName: String): Set<Team> {
        TODO("Not yet implemented")
    }

    override fun fetchTeamsMembers(teamId: String): Set<TeamMember> {
        TODO("Not yet implemented")
    }



    override fun validateRemoteConfig(organizationName: String) {
        //TODO("Not yet implemented")
    }

    override fun fetchOpenPRs(repositoryFullName: String): Set<PullRequest> {
        TODO("Not yet implemented")
    }


}

internal class AzureDevopsResponseDecoder {
    val log = LoggerFactory.getLogger(this.javaClass)

    val repoConfigMapper = ObjectMapper(YAMLFactory())

    init {
        repoConfigMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        repoConfigMapper.registerModule(KotlinModule.Builder().build())
    }

    fun decodeRepoConfig(response: okhttp3.Response): RepositoryConfig {

        if(response.code()==HttpStatus.NOT_FOUND.value()){
            return RepositoryConfig()
        }

        val writer = StringWriter()
        IOUtils.copy(response.body()?.byteStream(), writer, "UTF-8")
        val responseAsString = writer.toString()

        return parseRepositoryConfigResponse(responseAsString, response)
    }

    private fun parseRepositoryConfigResponse(responseAsString: String, response: okhttp3.Response): RepositoryConfig {
        if (responseAsString.isEmpty()) {
            return RepositoryConfig()
        }

        try {
            return repoConfigMapper.readValue(responseAsString, RepositoryConfig::class.java)
        } catch (e: IOException) {
            throw Repository.RepoConfigException(HttpStatus.BAD_REQUEST,"unable to parse config for repo - content : \"" + response.body() + "\"", e)
        }
    }

}

