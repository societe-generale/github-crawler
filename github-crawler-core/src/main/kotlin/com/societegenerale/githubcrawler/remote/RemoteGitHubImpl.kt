package com.societegenerale.githubcrawler.remote

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.societegenerale.githubcrawler.RepositoryConfig
import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.FileOnRepository
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.model.SearchResult
import com.societegenerale.githubcrawler.model.commit.Commit
import com.societegenerale.githubcrawler.model.commit.DetailedCommit
import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember
import feign.*
import feign.codec.Decoder
import feign.gson.GsonEncoder
import feign.httpclient.ApacheHttpClient
import feign.slf4j.Slf4jLogger
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.web.HttpMessageConverters
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder
import org.springframework.cloud.netflix.feign.support.SpringDecoder
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
class RemoteGitHubImpl @JvmOverloads constructor (val gitHubUrl: String, val usersReposInsteadOfOrgasRepos: Boolean = false, val oauthToken : String?) : RemoteGitHub {


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
            .decode404()
            .requestInterceptor(GitHubOauthTokenSetter(oauthToken))
            .logger(Slf4jLogger(RemoteGitHubImpl::class.java!!))
            .logLevel(Logger.Level.FULL)
            .target<InternalGitHubClient>(InternalGitHubClient::class.java!!, gitHubUrl)

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

        val reposUrl = "$gitHubUrl/"+userOrOrg()+"/$organizationName/repos"

        val requestBuilder = okhttp3.Request.Builder()
                .url(reposUrl)
                .header(ACCEPT, APPLICATION_GITHUB_MERCY_PREVIEW_JSON)

        addOAuthTokenIfRequired(requestBuilder)

        val request = requestBuilder.build()

        val response: Response

        try {
            response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw NoReachableRepositories("GET call to ${reposUrl} wasn't successful. Code : ${response.code()}, Message : ${response.message()}")
            }
        } catch (e: IOException) {
            throw NoReachableRepositories("Unable to perform the request",e)
        }

        return response
    }

    private fun addOAuthTokenIfRequired(requestBuilder : okhttp3.Request.Builder) : Unit{

        if(oauthToken!=null){
            requestBuilder.addHeader("Authorization", "token "+oauthToken)
        }

    }

    private fun userOrOrg() : String{
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

            val nextPageRequest=nextPageRequestBuilder.build()

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


    override fun fetchRepoBranches(organizationName: String, repositoryName: String): List<Branch> {

        return internalGitHubClient.fetchRepoBranches(organizationName, repositoryName)

    }

    /**
     * Because the GitHub API is a bit strange here, with the not very standard filter on repository, we need to build the request manually
     */
    override fun fetchCodeSearchResult(repositoryFullName: String, query: String): SearchResult {

        val urlBuilder = HttpUrl.parse(gitHubUrl + "/search/code")!!.newBuilder()
        urlBuilder.addQueryParameter("query", query)

        val searchCodeUrl = urlBuilder.build().toString()
        val searchCodeInRepoUrl = searchCodeUrl + "+repo:" + repositoryFullName

        val requestBuilder = okhttp3.Request.Builder()
                .url(searchCodeInRepoUrl)
                .header(ACCEPT, APPLICATION_JSON)

        addOAuthTokenIfRequired(requestBuilder)

        val request=requestBuilder.build()

        val response = httpClient.newCall(request).execute()

        return ObjectMapper().readValue(response.body().toString(), SearchResult::class.java)
    }

    override fun fetchFileContent(repositoryFullName: String, branchName: String, fileToFetch: String): String {

        val fileOnRepository: FileOnRepository

        try {
            fileOnRepository = internalGitHubClient.fetchFileOnRepo(repositoryFullName, branchName, fileToFetch)
        } catch (e: GitHubResponseDecoder.NoFileFoundFeignException) {
            //translating exception to a non Feign specific one
            throw NoFileFoundException("can't find $fileToFetch in repo $repositoryFullName, in branch $branchName")
        }


        val requestBuilder= okhttp3.Request.Builder()
                .url(fileOnRepository.downloadUrl)
                .header(ACCEPT, APPLICATION_JSON)

        addOAuthTokenIfRequired(requestBuilder)

        val request=requestBuilder.build()

        val response = httpClient.newCall(request).execute()


        return response.body()?.string() ?: ""

    }

    override fun fetchCommits(organizationName: String, repositoryFullName: String, perPage: Int): Set<Commit> {
        return internalGitHubClient.fetchCommits(organizationName, repositoryFullName, perPage)
    }

    override fun fetchCommit(organizationName: String, repositoryFullName: String, commitSha: String): DetailedCommit {
        return internalGitHubClient.fetchCommit(organizationName, repositoryFullName, commitSha)
    }

    override fun fetchTeams(token: String, organizationName: String): Set<Team> {
        return internalGitHubClient.fetchTeams(token, organizationName)
    }

    override fun fetchTeamsMembers(token: String, teamId: String): Set<TeamMember> {
        return internalGitHubClient.fetchTeamsMembers(token, teamId)
    }

    override fun fetchRepoConfig(repoFullName: String, defaultBranch: String): RepositoryConfig {

        val configFileOnRepository: FileOnRepository

        try {
            configFileOnRepository = internalGitHubClient.fetchFileOnRepo(repoFullName, defaultBranch, REPO_LEVEL_CONFIG_FILE)
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

        if(requestTemplate!=null && oauthToken!=null){
            requestTemplate.header("Authorization", "token "+oauthToken)
        }

    }

}


@Headers("Accept: application/json")
private interface InternalGitHubClient {

    @RequestLine("GET /repos/{organizationName}/{repositoryName}/branches")
    fun fetchRepoBranches(@Param("organizationName") organizationName: String,
                          @Param("repositoryName") repositoryName: String): List<Branch>

    @RequestLine("GET /repos/{repositoryFullName}/contents/{fileToFetch}?ref={branchName}")
    fun fetchFileOnRepo(@Param("repositoryFullName") repositoryFullName: String,
                        @Param("branchName") branchName: String,
                        @Param("fileToFetch") fileToFetch: String): FileOnRepository

    @RequestLine("GET /repos/{organizationName}/{repositoryFullName}/commits")
    fun fetchCommits(@Param("organizationName") organizationName: String,
                     @Param("repositoryFullName") repositoryFullName: String,
                     @Param("per_page") perPage: Int): Set<Commit>


    @RequestLine("GET /repos/{organizationName}/{repositoryFullName}/commits/{commitSha}")
    fun fetchCommit(@Param("organizationName") organizationName: String,
                    @Param("repositoryFullName") repositoryFullName: String,
                    @Param("commitSha") commitSha: String): DetailedCommit

    @RequestLine("GET /orgs/{organizationName}/teams")
    fun fetchTeams(@Param("access_token") token: String,
                   @Param("organizationName") organizationName: String): Set<Team>

    @RequestLine("GET /teams/{team}/members")
    fun fetchTeamsMembers(@Param("access_token") token: String,
                          @Param("team") teamId: String): Set<TeamMember>


}

internal class GitHubResponseDecoder : Decoder {
    val log = LoggerFactory.getLogger(this.javaClass)

    val repoConfigMapper = ObjectMapper(YAMLFactory())

    init {
        repoConfigMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        repoConfigMapper.registerModule(KotlinModule())
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
                throw NoFileFoundFeignException("problem while fetching content, of unknown type")
            }

        } else {

            log.debug("Decoding a successful response...")

            if (type.typeName == MediaType.TEXT_PLAIN_VALUE) {

                log.debug("\t ... as a String")

                return response.body().toString()
            }

            log.debug("\t ... as a " + type.typeName)

            val jacksonConverter = MappingJackson2HttpMessageConverter(ObjectMapper().registerModule(KotlinModule()))
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
            throw Repository.RepoConfigException("unable to parse config for repo - content : \"" + response.body() + "\"", e)
        }
    }

    class NoFileFoundFeignException(message: String) : FeignException(message)


}


