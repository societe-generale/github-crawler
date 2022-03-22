package com.societegenerale.githubcrawler.remote

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.societegenerale.githubcrawler.RepositoryConfig
import com.societegenerale.githubcrawler.model.*
import com.societegenerale.githubcrawler.model.commit.Commit
import com.societegenerale.githubcrawler.model.commit.DetailedCommit
import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember
import feign.*
import feign.FeignException.errorStatus
import feign.codec.Decoder
import feign.codec.ErrorDecoder
import feign.gson.GsonEncoder
import feign.httpclient.ApacheHttpClient
import feign.slf4j.Slf4jLogger
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder
import org.springframework.cloud.openfeign.support.SpringDecoder

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.Type


/**
 * Couple of methods have special behavior, so can't use purely annotation based Feign impl.
 * Implementation is mainly based on Feign's Builder for standard calls, and OkHttpClient for the others
 */
@Suppress("TooManyFunctions") // most of methods are one liners, implementing the methods declared in interface
class RemoteGitHubImpl @JvmOverloads constructor(val gitHubUrl: String, val usersReposInsteadOfOrgasRepos: Boolean = false, val oauthToken: String) : RemoteGitHub {

    companion object {
        const val REPO_LEVEL_CONFIG_FILE = ".githubCrawler"
        const val APPLICATION_JSON = "application/json"
        const val ACCEPT = "accept"
        const val CONFIG_VALIDATION_REQUEST_HEADER = "X-configValidationRequest"
        const val APPLICATION_GITHUB_MERCY_PREVIEW_JSON = "application/vnd.github.mercy-preview+json"
    }

    private val internalGitHubClient: InternalGitHubClient = Feign.builder()
        .client(ApacheHttpClient())
        .encoder(GsonEncoder())
        .decoder(GitHubResponseDecoder())
        .errorDecoder(GiHubErrorDecoder())
        .decode404()
        .requestInterceptor(GitHubOauthTokenSetter(oauthToken))
        .logger(Slf4jLogger(RemoteGitHubImpl::class.java))
        .logLevel(Logger.Level.FULL)
        .target<InternalGitHubClient>(InternalGitHubClient::class.java, gitHubUrl)

    private val httpClient = OkHttpClient()


    val log = LoggerFactory.getLogger(this.javaClass)

    private val objectMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Throws(NoReachableRepositories::class)
    override fun validateRemoteConfig(organizationName: String) {

        val response = performFirstCall(organizationName, isConfigCall = true)

        try {
            extractRepositories(response)
        } catch (e: JsonProcessingException) {
            throw NoReachableRepositories("not able to parse response : ${response.body()}", e)
        }

    }

    @Throws(NoReachableRepositories::class)
    private fun performFirstCall(organizationName: String, isConfigCall: Boolean = false): Response {

        val reposUrl = "$gitHubUrl/" + userOrOrg() + "/$organizationName/repos"

        val requestBuilder = okhttp3.Request.Builder()
            .url(reposUrl)
            .header(ACCEPT, APPLICATION_GITHUB_MERCY_PREVIEW_JSON)

        addOAuthTokenIfRequired(requestBuilder)

        if (isConfigCall) {
            requestBuilder.addHeader(CONFIG_VALIDATION_REQUEST_HEADER, "true")
        }

        val request = requestBuilder.build()

        val response: Response

        try {
            response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw NoReachableRepositories("GET call to ${reposUrl} wasn't successful. Code : ${response.code()}, Message : ${response.message()}")
            }
        } catch (e: IOException) {
            throw NoReachableRepositories("Unable to perform the request", e)
        }

        return response
    }

    private fun addOAuthTokenIfRequired(requestBuilder: okhttp3.Request.Builder): Unit {

        if (oauthToken.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "token " + oauthToken)
        }

    }

    private fun userOrOrg(): String {
        return if (usersReposInsteadOfOrgasRepos) "users" else "orgs"
    }

    override fun fetchRepositories(organizationName: String): Set<Repository> {

        val repositoriesFromOrga = HashSet<Repository>()

        val response = performFirstCall(organizationName)

        repositoriesFromOrga.addAll(extractRepositories(response))

        var nextPageLink = getLinkToNextPageIfAny(response)

        var pageNb = 1

        while (nextPageLink != null) {

            pageNb++


            val nextPageRequestBuilder = okhttp3.Request.Builder()
                .url(nextPageLink)
                .header(ACCEPT, APPLICATION_GITHUB_MERCY_PREVIEW_JSON)

            addOAuthTokenIfRequired(nextPageRequestBuilder)

            val nextPageRequest = nextPageRequestBuilder.build()

            val nextPageResponse = httpClient.newCall(nextPageRequest).execute()

            val nextPageRepositories = extractRepositories(nextPageResponse)

            log.info("nb of repositories in $organizationName organization, page {} : {}", pageNb, nextPageRepositories.size)

            repositoriesFromOrga.addAll(nextPageRepositories)
            log.info("total nb of repositories in $organizationName organization so far : {}", nextPageRepositories.size)

            nextPageLink = getLinkToNextPageIfAny(nextPageResponse)

        }

        return repositoriesFromOrga
    }

    private fun extractRepositories(response: Response): Set<Repository> {

        try {

            val body = response.body()

            if (body != null) {
                return objectMapper.readValue(body.string())
            } else {
                log.warn("response is null : {}", response)
                return emptySet()
            }
        } catch (e: JsonProcessingException) {
            throw NoReachableRepositories("not able to parse response", e)
        }
    }

    private fun getLinkToNextPageIfAny(response: Response): String? {

        val linksFromHeader = response.header("link")

        if (linksFromHeader != null) {

            val links = linksFromHeader.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (i in links.indices) {
                val link = links[i]
                if (link.contains("rel=\"next\"")) {

                    return link.substring(link.indexOf("<") + 1, link.lastIndexOf(">"))
                }
            }
        }
        return null
    }


    override fun fetchRepoBranches(repositoryFullName: String): Set<Branch> {

        return internalGitHubClient.fetchRepoBranches(repositoryFullName)

    }

    override fun fetchOpenPRs(repositoryFullName: String): Set<PullRequest> {
        return internalGitHubClient.fetchOpenPRs(repositoryFullName)
    }

    /**
     * Because the GitHub API is a bit strange here, with the not very standard filter on repository, we need to build the request manually
     */
    override fun fetchCodeSearchResult(repositoryFullName: String, query: String): SearchResult {

        val searchCodeUrl = HttpUrl.parse(gitHubUrl +buildQueryString(query,repositoryFullName))!!.newBuilder().build().toString()

        log.info("fetching code search result from $searchCodeUrl");

        val requestBuilder = okhttp3.Request.Builder()
            .url(searchCodeUrl)
            .header(ACCEPT, APPLICATION_JSON)

        addOAuthTokenIfRequired(requestBuilder)

        val request = requestBuilder.build()

        val response = httpClient.newCall(request).execute()

        val responseAsString=response.body()?.string()
        log.info("response : "+responseAsString)

        return try {
            objectMapper.readValue(responseAsString, SearchResult::class.java)
        }
        catch(e : JsonParseException){
            log.warn("parsing error",e)
            SearchResult(0, emptyList())
        }
    }

    private fun buildQueryString(queryString: String, repositoryFullName: String): String {
        return "/search/code?q=$queryString repo:${repositoryFullName}"
    }

    override fun fetchFileContent(repositoryFullName: String, branchName: String, fileToFetch: String): String {

        val fileOnRepository: FileOnRepository

        try {
            fileOnRepository = internalGitHubClient.fetchFileOnRepo(repositoryFullName, branchName, fileToFetch)
        } catch (e: GitHubResponseDecoder.NoFileFoundFeignException) {
            //translating exception to a non Feign specific one
            throw NoFileFoundException("can't find $fileToFetch in repo $repositoryFullName, in branch $branchName")
        }

        val requestBuilder = okhttp3.Request.Builder()
            .url(fileOnRepository.downloadUrl)
            .header(ACCEPT, APPLICATION_JSON)

        addOAuthTokenIfRequired(requestBuilder)

        val request = requestBuilder.build()

        val response = httpClient.newCall(request).execute()


        return response.body()?.string() ?: ""

    }

    override fun fetchCommits(repositoryFullName: String, perPage: Int): Set<Commit> {

        return try {
            internalGitHubClient.fetchCommits(repositoryFullName, perPage)
        }
        catch (e: GitHubResponseDecoder.GithubException) {
            log.warn("not able to fetch commits for repo $repositoryFullName",e)
            emptySet()
        }

    }

    override fun fetchCommit(repositoryFullName: String, commitSha: String): DetailedCommit {
        return internalGitHubClient.fetchCommit(repositoryFullName, commitSha)
    }

    override fun fetchTeams(organizationName: String): Set<Team> {
        return internalGitHubClient.fetchTeams(organizationName)
    }

    override fun fetchTeamsMembers(teamId: String): Set<TeamMember> {
        return internalGitHubClient.fetchTeamsMembers(teamId)
    }

    override fun fetchRepoConfig(repositoryFullName: String, defaultBranch: String): RepositoryConfig {

        val configFileOnRepository: FileOnRepository

        try {
            configFileOnRepository = internalGitHubClient.fetchFileOnRepo(repositoryFullName, defaultBranch, REPO_LEVEL_CONFIG_FILE)
        } catch (e: GitHubResponseDecoder.NoFileFoundFeignException) {
            return RepositoryConfig()
        }

        val requestBuilder = okhttp3.Request.Builder()
            .url(configFileOnRepository.downloadUrl)
            .header(ACCEPT, APPLICATION_JSON)

        addOAuthTokenIfRequired(requestBuilder)

        val request = requestBuilder.build()

        val response = httpClient.newCall(request).execute()

        val decoder = GitHubResponseDecoder()

        return decoder.decodeRepoConfig(response)
    }

}

class GitHubOauthTokenSetter(val oauthToken: String?) : RequestInterceptor {

    override fun apply(requestTemplate: RequestTemplate?) {

        if (requestTemplate != null && oauthToken != null) {
            requestTemplate.header("Authorization", "token " + oauthToken)
        }

    }

}


@Headers("Accept: application/json")
private interface InternalGitHubClient {

    @RequestLine("GET /repos/{fullName}/branches")
    fun fetchRepoBranches(@Param("fullName") fullName: String): Set<Branch>

    @RequestLine("GET /repos/{repositoryFullName}/contents/{fileToFetch}?ref={branchName}")
    fun fetchFileOnRepo(@Param("repositoryFullName") repositoryFullName: String,
                        @Param("branchName") branchName: String,
                        @Param("fileToFetch") fileToFetch: String): FileOnRepository

    @RequestLine("GET /repos/{repositoryFullName}/commits")
    fun fetchCommits(@Param("repositoryFullName") repositoryFullName: String,
                     @Param("per_page") perPage: Int): Set<Commit>


    @RequestLine("GET /repos/{repositoryFullName}/commits/{commitSha}")
    fun fetchCommit(@Param("repositoryFullName") repositoryFullName: String,
                    @Param("commitSha") commitSha: String): DetailedCommit

    @RequestLine("GET /orgs/{organizationName}/teams")
    fun fetchTeams(@Param("organizationName") organizationName: String): Set<Team>

    @RequestLine("GET /teams/{team}/members")
    fun fetchTeamsMembers(@Param("team") teamId: String): Set<TeamMember>

    @RequestLine("GET /repos/{fullName}/pulls?state=open")
    fun fetchOpenPRs(@Param("fullName") fullName: String): Set<PullRequest>


}

internal class GiHubErrorDecoder : ErrorDecoder {

    override fun decode(methodKey: String?, response: feign.Response?): java.lang.Exception {

        if (response?.status() == HttpStatus.CONFLICT.value()) {
            throw GitHubResponseDecoder.GithubException("problem while fetching content... conflict state as per HTTP 409 code")
        }

        return errorStatus(methodKey, response);
    }
}

internal class GitHubResponseDecoder : Decoder {
    val log = LoggerFactory.getLogger(this.javaClass)

    val repoConfigMapper = ObjectMapper(YAMLFactory())

    init {
        repoConfigMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        repoConfigMapper.registerModule(KotlinModule.Builder().build())
    }

    fun decodeRepoConfig(response: okhttp3.Response): RepositoryConfig {

        val writer = StringWriter()
        IOUtils.copy(response.body()?.byteStream(), writer, "UTF-8")
        val responseAsString = writer.toString()

        return parseRepositoryConfigResponse(responseAsString, response)
    }


    @Throws(IOException::class)
    override fun decode(response: feign.Response, type: Type): Any {

        if (response.status() == HttpStatus.NOT_FOUND.value()) {

            if (type.typeName == FileOnRepository::class.java.name) {
                throw NoFileFoundFeignException("no file found on repository")
            } else {
                throw NoFileFoundFeignException("problem while fetching content, of unknown type ${type.typeName}")
            }
        }
        else {

            log.debug("Decoding a successful response...")

            if (type.typeName == MediaType.TEXT_PLAIN_VALUE) {

                log.debug("\t ... as a String")

                return response.body().toString()
            }

            log.debug("\t ... as a " + type.typeName)

            val jacksonConverter = MappingJackson2HttpMessageConverter(ObjectMapper().registerModule(KotlinModule.Builder().build()))
            val objectFactory = { HttpMessageConverters(jacksonConverter) }
            return ResponseEntityDecoder(SpringDecoder(objectFactory)).decode(response, type)

        }
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

    class NoFileFoundFeignException(message: String) : FeignException(HttpStatus.NOT_FOUND.value(),message)

    class GithubException(message: String) : FeignException(HttpStatus.INTERNAL_SERVER_ERROR.value(),message)

}


