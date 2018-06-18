package com.societegenerale.githubcrawler.remote

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
class RemoteGitHubImpl(val gitHubUrl: String) : RemoteGitHub {


    companion object {
        const val REPO_LEVEL_CONFIG_FILE = ".githubCrawler"
    }

    private val internalGitHubClient: InternalGitHubClient = Feign.builder()
            .client(ApacheHttpClient())
            .encoder(GsonEncoder())
            .decoder(GitHubResponseDecoder())
            .decode404()
            .logger(Slf4jLogger(RemoteGitHubImpl::class.java!!))
            .logLevel(Logger.Level.FULL)
            .target<InternalGitHubClient>(InternalGitHubClient::class.java!!, gitHubUrl)

    private val httpClient = OkHttpClient()

    val log = LoggerFactory.getLogger(this.javaClass)

    private val objectMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun fetchRepositories(organizationName: String): Set<Repository> {

        val repositoriesFromOrga = HashSet<Repository>()

        val request = okhttp3.Request.Builder()
                .url(gitHubUrl + "/orgs/$organizationName/repos")
                .header("Accept", "application/json")
                .build()

        val response = httpClient.newCall(request).execute()

        repositoriesFromOrga.addAll(extractRepositories(response))

        var nextPageLink = getLinkToNextPageIfAny(response)

        var pageNb = 1

        while (nextPageLink != null) {

            pageNb++


            val nextPageRequest = okhttp3.Request.Builder()
                    .url(nextPageLink)
                    .header("Accept", "application/json")
                    .build()

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

        //TODO if issue in URL, we'll have a cproblem here - should catch it and log nicely what the issue is
        val body = response.body()

        if (body != null) {
            return objectMapper.readValue(body.string())
        } else {
            log.warn("response is null : {}", response)
            return emptySet()
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

        val request = okhttp3.Request.Builder()
                .url(searchCodeInRepoUrl)
                .header("Accept", "application/json")
                .build()

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


        val request = okhttp3.Request.Builder()
                .url(fileOnRepository.downloadUrl)
                .header("Accept", "application/json")
                .build()

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

        val request = okhttp3.Request.Builder()
                .url(configFileOnRepository.downloadUrl)
                .header("Accept", "application/json")
                .build()

        val response = httpClient.newCall(request).execute()

        val decoder = GitHubResponseDecoder()

        return decoder.decodeRepoConfig(response)
    }

    override fun fetchFileOnRepo(repositoryFullName: String, branchName: String, fileToFetch: String): FileOnRepository? {
        return internalGitHubClient.fetchFileOnRepo(repositoryFullName, branchName, fileToFetch)
    }

}


@Headers("Accept: application/json")
private interface InternalGitHubClient {

//    companion object {
//        const val REPO_LEVEL_CONFIG_FILE = ".githubCrawler"
//    }

    @RequestLine("GET /repos/{organizationName}/{repositoryName}/branches")
    fun fetchRepoBranches(@Param("organizationName") organizationName: String,
                          @Param("repositoryName") repositoryName: String): List<Branch>


//    @RequestLine("GET /raw/{repoFullName}/{defaultBranch}/" + REPO_LEVEL_CONFIG_FILE)
//    fun fetchRepoConfig(@Param("repoFullName") repoFullName: String,
//                        @Param("defaultBranch") defaultBranch: String): RepositoryConfig


//    @RequestLine("GET /raw/{repositoryFullName}/{branchName}/{fileToFetch}")
//    fun fetchFileContent(@Param("repositoryFullName") repositoryFullName: String,
//                         @Param("branchName") branchName: String,
//                         @Param("fileToFetch") fileToFetch: String): String


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
    @Headers("Authorization: {access_token}")
    fun fetchTeams(@Param("access_token") token: String,
                   @Param("organizationName") organizationName: String): Set<Team>

    @RequestLine("GET /teams/{team}/members")
    @Headers("Authorization: {access_token}")
    fun fetchTeamsMembers(@Param("access_token") token: String,
                          @Param("team") teamId: String): Set<TeamMember>


}

internal class GitHubResponseDecoder : Decoder {
    val log = LoggerFactory.getLogger(this.javaClass)

    val mapper = ObjectMapper(YAMLFactory())

    init {
        mapper.registerModule(KotlinModule())
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
            }

            //TODO check if still required
            if (type.typeName == RepositoryConfig::class.java.name) {
                throw NoRepoConfigFileFoundException("no repository level config found")
            }

            log.info("no file found ")
            throw Repository.RepoConfigException("problem while fetching")

        } else {

            log.debug("Decoding a successful response...")

            if (type.typeName == MediaType.TEXT_PLAIN_VALUE) {

                log.debug("\t ... as a String")

                return response.body().toString()
            }

//            else if (type.typeName == RepositoryConfig::class.java.name) {
//
//                log.debug("\t ... as a specific " + RepositoryConfig::class.java.name)
//
//                val responseAsString = extractResponseBodyAsString(response)
//
//                return parseRepositoryConfigResponse(responseAsString, response)
//
//            }

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
            return mapper.readValue(responseAsString, RepositoryConfig::class.java)
        } catch (e: IOException) {
            throw Repository.RepoConfigException("unable to parse config for repo - content : \"" + response.body() + "\"", e)
        }
    }


    class NoRepoConfigFileFoundException(message: String) : FeignException(message)

    class NoFileFoundFeignException(message: String) : FeignException(message)


}


