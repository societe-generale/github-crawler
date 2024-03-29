package com.societegenerale.githubcrawler.remote

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.societegenerale.githubcrawler.RepositoryConfig
import com.societegenerale.githubcrawler.model.*
import com.societegenerale.githubcrawler.model.Author
import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.PullRequest
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.model.bitbucket.*
import com.societegenerale.githubcrawler.model.commit.Commit
import com.societegenerale.githubcrawler.model.commit.CommitStats
import com.societegenerale.githubcrawler.model.commit.DetailedCommit
import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember
import feign.*
import feign.codec.Decoder
import feign.gson.GsonEncoder
import feign.httpclient.ApacheHttpClient
import feign.slf4j.Slf4jLogger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.io.IOException
import java.lang.reflect.Type
import java.util.*
import java.util.stream.Collectors

@Suppress("TooManyFunctions")
class RemoteBitBucketImpl @JvmOverloads constructor(
    bitBucketUrl: String,
    private val organizationName: String = "",
    private val apiKey: String
) : RemoteSourceControl {

    companion object {
        const val REPO_LEVEL_CONFIG_FILE = ".BitBucketCrawler"
    }

    private val internalBitBucketClient: InternalBitBucketClient = Feign.builder()
        .client(ApacheHttpClient())
        .encoder(GsonEncoder())
        .decoder(BitBucketResponseDecoder())
        .decode404()
        .requestInterceptor(BitBucketOauthTokenSetter(apiKey))
        .logger(Slf4jLogger(RemoteBitBucketImpl::class.java))
        .logLevel(Logger.Level.FULL)
        .target<InternalBitBucketClient>(InternalBitBucketClient::class.java, bitBucketUrl)


    val log = LoggerFactory.getLogger(this.javaClass)

    private val objectMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun validateRemoteConfig(organizationName: String) {
        //TODO("Not yet implemented")
    }

    @Throws(NoReachableRepositories::class)
    private fun performFirstCall(organizationName: String): Repositories {
        try {
            return internalBitBucketClient.fetchRepos(organizationName, 0)

        } catch (e: IOException) {
            throw NoReachableRepositories("Unable to perform the request", e)
        }
    }

    override fun fetchRepositories(organizationName: String): Set<Repository> {

        val repositoriesFromOrga = HashSet<Repository>()

        var response = performFirstCall(organizationName)

        repositoriesFromOrga.addAll(extractRepositories(response))

        while (!response.isLastPage) {
            log.info("total nb of repositories in $organizationName organization so far : {}", response.values.size)

            response = getRepoIfAny(organizationName, response.nextPageStart)

            repositoriesFromOrga.addAll(extractRepositories(response))
        }

        return repositoriesFromOrga
    }

    private fun extractRepositories(repositories: Repositories): Set<Repository> {
        return repositories.values.stream().map { repo ->
            Repository(
                url = repo.links.self.first().href,
                name = repo.name,
                fullName = repo.fullName,
                defaultBranch = repo.defaultBranch,
                creationDate = Date(),
                lastUpdateDate = Date()

            )
        }
            .collect(Collectors.toSet())
    }

    private fun getRepoIfAny(organizationName: String, nextPageStart: Int): Repositories {
        return internalBitBucketClient.fetchRepos(organizationName, nextPageStart)
    }

    override fun fetchRepoBranches(repositoryFullName: String): Set<Branch> {
        return internalBitBucketClient.fetchRepoBranches(organizationName, repositoryFullName).values.stream()
            .map { Branch(it.displayId) }.collect(Collectors.toSet())
    }

    override fun fetchOpenPRs(repositoryFullName: String): Set<PullRequest> {
        return internalBitBucketClient.fetchOpenPRs(organizationName, repositoryFullName).values.stream()
            .map { PullRequest(it.id) }.collect(Collectors.toSet())
    }

    override fun fetchCodeSearchResult(repositoryFullName: String, query: String): SearchResult {
        throw NotImplementedError("Bitbucket REST API is not available for code search")
    }

    override fun fetchFileContent(repositoryFullName: String, branchName: String, fileToFetch: String): String {

        try {
            return internalBitBucketClient.fetchFileOnRepo(organizationName, repositoryFullName, branchName, fileToFetch)
        } catch (e: BitBucketResponseDecoder.NoFileFoundFeignException) {
            //translating exception to a non Feign specific one
            throw NoFileFoundException("can't find $fileToFetch in repo $repositoryFullName, in branch $branchName")
        }
    }

    override fun fetchCommits(repositoryFullName: String, perPage: Int): Set<Commit> {

        return try {
            internalBitBucketClient.fetchCommits(organizationName, repositoryFullName, perPage).values.stream()
                .map { Commit(it.id) }.collect(Collectors.toSet())
        } catch (e: BitBucketResponseDecoder.BitBucketException) {
            log.warn("not able to fetch commits for repo $repositoryFullName", e)
            emptySet()
        }

    }

    override fun fetchCommit(repositoryFullName: String, commitSha: String): DetailedCommit {
        val commit = internalBitBucketClient.fetchCommit(organizationName, repositoryFullName, commitSha)
        //TODO total
        return DetailedCommit(commit.id, Author(commit.author.id, commit.author.emailAddress), CommitStats(0))
    }

    override fun fetchTeams(organizationName: String): Set<Team> {
        throw NotImplementedError()
    }

    override fun fetchTeamsMembers(teamId: String): Set<TeamMember> {
        throw NotImplementedError()
    }

    override fun fetchRepoConfig(repositoryFullName: String, defaultBranch: String): RepositoryConfig {

        val content: String

        try {
            content = internalBitBucketClient.fetchFileOnRepo(
                organizationName,
                repositoryFullName,
                defaultBranch,
                REPO_LEVEL_CONFIG_FILE
            )
        } catch (e: BitBucketResponseDecoder.NoFileFoundFeignException) {
            return RepositoryConfig()
        }
        val decoder = BitBucketResponseDecoder()

        return decoder.decodeRepoConfig(content)
    }

}

class BitBucketOauthTokenSetter(private val oauthToken: String) : RequestInterceptor {

    override fun apply(requestTemplate: RequestTemplate?) {
        requestTemplate?.header("Authorization", "Bearer " + oauthToken)
    }

}


@Headers("Accept: application/json")
private interface InternalBitBucketClient {

    @RequestLine("GET /projects/{organizationName}/repos/{fullName}/branches")
    fun fetchRepoBranches(@Param("organizationName") organizationName: String, @Param("fullName") fullName: String): Branches

    @RequestLine("GET /projects/{organizationName}/repos?start={start}")
    fun fetchRepos(@Param("organizationName") organizationName: String, @Param("start") start: Int): Repositories

    @RequestLine("GET /projects/{organizationName}/repos/{repositoryFullName}/raw/{fileToFetch}?at={branchName}")
    fun fetchFileOnRepo(
        @Param("organizationName") organizationName: String,
        @Param("repositoryFullName") repositoryFullName: String,
        @Param("branchName") branchName: String,
        @Param("fileToFetch") fileToFetch: String
    ): String

    @RequestLine("GET /projects/{organizationName}/repos/{repositoryFullName}/commits?limit={limit}")
    fun fetchCommits(
        @Param("organizationName") organizationName: String,
        @Param("repositoryFullName") repositoryFullName: String,
        @Param("limit") limit: Int
    ): Commits

    @RequestLine("GET /projects/{organizationName}/repos/{repositoryFullName}/commits/{commitSha}")
    fun fetchCommit(
        @Param("organizationName") organizationName: String,
        @Param("repositoryFullName") repositoryFullName: String,
        @Param("commitSha") commitSha: String
    ): com.societegenerale.githubcrawler.model.bitbucket.DetailedCommit

    @RequestLine("GET /admin/groups")
    fun fetchTeams(@Param("organizationName") organizationName: String): Groups

    @RequestLine("GET /admin/groups/{teamId}")
    fun fetchTeamsMembers(@Param("teamId") teamId: String): Set<TeamMember>

    @RequestLine("GET /projects/{organizationName}/repos/{fullName}/pull-requests")
    fun fetchOpenPRs(@Param("organizationName") organizationName: String, @Param("fullName") fullName: String): PullRequests
}

internal class BitBucketResponseDecoder : Decoder {
    val log = LoggerFactory.getLogger(this.javaClass)

    val repoConfigMapper = ObjectMapper(YAMLFactory())

    init {
        repoConfigMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        repoConfigMapper.registerModule(KotlinModule.Builder().build())
    }

    fun decodeRepoConfig(response: String): RepositoryConfig {

        return parseRepositoryConfigResponse(response)
    }


    @Throws(IOException::class)
    override fun decode(response: Response, type: Type): Any {

        if (response.status() == HttpStatus.NOT_FOUND.value()) {

            if (type.typeName == FileOnRepository::class.java.name) {
                throw NoFileFoundFeignException("no file found on repository")
            } else {
                throw NoFileFoundFeignException("problem while fetching content, of unknown type ${type.typeName}")
            }
        } else {

            log.debug("Decoding a successful response...")

            if (type.typeName == MediaType.TEXT_PLAIN_VALUE) {

                log.debug("\t ... as a String")

                return response.body().toString()
            }

            log.debug("\t ... as a " + type.typeName)

            val jacksonConverter =
                MappingJackson2HttpMessageConverter(ObjectMapper().registerModule(KotlinModule.Builder().build()))
            val objectFactory = { HttpMessageConverters(jacksonConverter) }
            return ResponseEntityDecoder(SpringDecoder(objectFactory)).decode(response, type)

        }
    }

    private fun parseRepositoryConfigResponse(responseAsString: String): RepositoryConfig {
        if (responseAsString.isEmpty()) {
            return RepositoryConfig()
        }

        try {
            return repoConfigMapper.readValue(responseAsString, RepositoryConfig::class.java)
        } catch (e: IOException) {
            throw Repository.RepoConfigException(
                HttpStatus.BAD_REQUEST,
                "unable to parse config for repo - content : \"" + responseAsString + "\"",
                e
            )
        }
    }

    class NoFileFoundFeignException(message: String) : FeignException(HttpStatus.NOT_FOUND.value(), message)

    class BitBucketException(message: String) : FeignException(HttpStatus.INTERNAL_SERVER_ERROR.value(), message)

}


