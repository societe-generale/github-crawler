package com.societegenerale.githubcrawler.remote

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.societegenerale.githubcrawler.RepositoryConfig
import com.societegenerale.githubcrawler.model.*
import com.societegenerale.githubcrawler.model.azuredevops.Repositories
import com.societegenerale.githubcrawler.model.commit.Commit
import com.societegenerale.githubcrawler.model.commit.DetailedCommit
import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.io.IOException
import java.io.StringWriter
import java.util.*
import java.util.stream.Collectors.toSet


class RemoteAzureDevopsImpl @JvmOverloads constructor(val azureDevopsUrl: String = AZURE_DEVOPS_URL,
                                                      val azureDevopsSearchUrl: String = AZURE_DEVOPS_SEARCH_URL,
                                                      organization: String,
                                                      personalAccessToken: String) : RemoteSourceControl {

    var log = LoggerFactory.getLogger(this.javaClass.toString())

    companion object {

        const val AZURE_DEVOPS_URL= "https://dev.azure.com/"

        const val AZURE_DEVOPS_SEARCH_URL= "https://almsearch.dev.azure.com/"

        const val AZURE_DEVOPS_API_VERSION= "api-version=7.1-preview.1"

        const val REPO_LEVEL_CONFIG_FILE = ".azureDevopsCrawler"

    }

    private val splitedOrgName=organization.split("#")
    private var azureOrg=splitedOrgName.get(0)
    private var azureProject=splitedOrgName.get(1)

    private val objectMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

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

        log.info("fetch repositories from "+this.toString())

        val repositoriesUrl=azureDevopsUrl+"${azureOrg}/${azureProject}/_apis/git/repositories?${AZURE_DEVOPS_API_VERSION}"

        val request = requestTemplate.url(repositoriesUrl).build()

        val responseBody=httpClient.newCall(request).execute().body

        val repositories = readRepositories(responseBody)

        return repositories.value.stream().map{ repo -> Repository(
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

    private fun readRepositories(responseBody: ResponseBody?): Repositories {

        var bodyAsString : String? = ""

        try{

            bodyAsString = responseBody?.string()

            return objectMapper.readValue(bodyAsString, Repositories::class.java)
        }
        catch( e : JsonParseException){

            throw IllegalArgumentException("unable to read the repositories to crawl from the response we got $bodyAsString",e)
        }
    }


    override fun fetchRepoConfig(repositoryFullName: String, defaultBranch: String): RepositoryConfig {

        val configUrl=azureDevopsUrl+"${azureOrg}/${azureProject}/_apis/git/repositories/${repositoryFullName}/items?path=${REPO_LEVEL_CONFIG_FILE}&${
            AZURE_DEVOPS_API_VERSION}"

        log.info("fetching optional repo config from $configUrl")


        val request = requestTemplate.url(configUrl).build()

        val response=httpClient.newCall(request).execute()

        val decoder = AzureDevopsResponseDecoder()

        return decoder.decodeRepoConfig(response)
    }

    override fun fetchRepoBranches(repositoryFullName: String): Set<Branch> {
        TODO("Not yet implemented")
    }

    override fun fetchCodeSearchResult(repositoryFullName: String, query: String): SearchResult {

        val fileSearchUrl=azureDevopsSearchUrl+"$azureOrg/_apis/search/codesearchresults?${AZURE_DEVOPS_API_VERSION}"

        val codeSearchRequestDetails = CodeSearchRequestDetails(query,CodeSearchFilter(listOf(repositoryFullName),listOf(azureProject)))

        //TODO hack-ish way to build the payload : use custom serializer instead
        val requestPayloadAsString=objectMapper.writeValueAsString(codeSearchRequestDetails).replaceFirst("{","{\"\$top\": 1,")

        val codeSearchBody = requestPayloadAsString.toRequestBody("application/json".toMediaTypeOrNull())

        val request =requestTemplate.url(fileSearchUrl).post(codeSearchBody).build()

        val responseBody= httpClient.newCall(request).execute().body

        val repoSearchResult = objectMapper.readValue(responseBody?.string(), CodeSearchResult::class.java)

        return repoSearchResult.toStandardSearchResult()

    }

    override fun fetchFileContent(repositoryFullName: String, branchName: String, fileToFetch: String): String {
        val fileContentUrl=azureDevopsUrl+"$azureOrg/$azureProject/_apis/git/repositories/$repositoryFullName/items?" +
            "path=${fileToFetch}" +
            "&versionDescriptor.versionType=branch" +
            "&versionDescriptor.version=" +extractBranchNameFromRef(branchName) +
            "&${AZURE_DEVOPS_API_VERSION}"

        val request = requestTemplate.url(fileContentUrl).build()

        val response=httpClient.newCall(request).execute()

        if(response.isSuccessful){
            return response.body?.string() ?: ""
        }
        else{
            throw NoFileFoundException("can't find $fileToFetch in repo $repositoryFullName, in branch $branchName")
        }

    }

    /**
     * the branchName we receive is actually the reference, like "refs/heads/main". We need only the part after the "/"
     */
    private fun extractBranchNameFromRef(refName: String): String {
        return refName.substring(refName.lastIndexOf("/")+1)
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

internal data class CodeSearchResult(val count : Int, val results : List<CodeSearchResultItem>) {

    fun toStandardSearchResult(): SearchResult {

        return SearchResult(count,results.map { SearchResultItem(toStandardPath(it.path)) }.toList())
    }

    private fun toStandardPath(path: String): String {

        if (path.startsWith("/")) {
            return path.substring(1)
        } else {
            return path
        }

    }


}

internal class CodeSearchResultItem(val path : String)


internal class AzureDevopsResponseDecoder {
    val log = LoggerFactory.getLogger(this.javaClass)

    val repoConfigMapper = ObjectMapper(YAMLFactory())

    init {
        repoConfigMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        repoConfigMapper.registerModule(KotlinModule.Builder().build())
    }

    fun decodeRepoConfig(response: okhttp3.Response): RepositoryConfig {

        if(response.code==HttpStatus.NOT_FOUND.value()){
            return RepositoryConfig()
        }

        val writer = StringWriter()
        IOUtils.copy(response.body?.byteStream(), writer, "UTF-8")
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
            throw Repository.RepoConfigException(HttpStatus.BAD_REQUEST,"unable to parse config for repo - content : \"" + response.body + "\"", e)
        }
    }

}

