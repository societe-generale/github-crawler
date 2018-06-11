package com.societegenerale.githubcrawler.remote

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.societegenerale.githubcrawler.RepositoryConfig
import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.model.SearchResult
import com.societegenerale.githubcrawler.model.commit.Commit
import com.societegenerale.githubcrawler.model.commit.DetailedCommit
import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember
import feign.*
import feign.gson.GsonEncoder
import feign.httpclient.ApacheHttpClient
import feign.slf4j.Slf4jLogger
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import org.slf4j.LoggerFactory


/**
 * Couple of methods have special behavior, so can't use purely annotation based Feign impl.
 * Implementation is mainly based on Feign's Builder for standard calls, and OkHttpClient for the others
 */
@Suppress("TooManyFunctions") // most of methods are one liners, implementing the methods declared in interface
class RemoteGitHubImpl(val gitHubUrl: String) : RemoteGitHub {

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
                .url(gitHubUrl + "/api/v3/orgs/$organizationName/repos")
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

        val urlBuilder = HttpUrl.parse(gitHubUrl + "/api/v3/search/code")!!.newBuilder()
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
        return internalGitHubClient.fetchFileContent(repositoryFullName, branchName, fileToFetch)
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
        return internalGitHubClient.fetchRepoConfig(repoFullName, defaultBranch)
    }

}


@Headers("Accept: application/json")
private interface InternalGitHubClient {

    companion object {
        const val REPO_LEVEL_CONFIG_FILE = ".githubCrawler"
    }

    @RequestLine("GET /api/v3/repos/{organizationName}/{repositoryName}/branches")
    fun fetchRepoBranches(@Param("organizationName") organizationName: String,
                          @Param("repositoryName") repositoryName: String): List<Branch>


    @RequestLine("GET /raw/{repoFullName}/{defaultBranch}/" + REPO_LEVEL_CONFIG_FILE)
    fun fetchRepoConfig(@Param("repoFullName") repoFullName: String,
                        @Param("defaultBranch") defaultBranch: String): RepositoryConfig


    @RequestLine("GET /raw/{repositoryFullName}/{branchName}/{fileToFetch}")
    fun fetchFileContent(@Param("repositoryFullName") repositoryFullName: String,
                         @Param("branchName") branchName: String,
                         @Param("fileToFetch") fileToFetch: String): String


    @RequestLine("GET /api/v3/repos/{organizationName}/{repositoryFullName}/commits")
    fun fetchCommits(@Param("organizationName") organizationName: String,
                     @Param("repositoryFullName") repositoryFullName: String,
                     @Param("per_page") perPage: Int): Set<Commit>


    @RequestLine("GET /api/v3/repos/{organizationName}/{repositoryFullName}/commits/{commitSha}")
    fun fetchCommit(@Param("organizationName") organizationName: String,
                    @Param("repositoryFullName") repositoryFullName: String,
                    @Param("commitSha") commitSha: String): DetailedCommit

    @RequestLine("GET /api/v3/orgs/{organizationName}/teams")
    @Headers("Authorization: {access_token}")
    fun fetchTeams(@Param("access_token") token: String,
                   @Param("organizationName") organizationName: String): Set<Team>

    @RequestLine("GET /api/v3/teams/{team}/members")
    @Headers("Authorization: {access_token}")
    fun fetchTeamsMembers(@Param("access_token") token: String,
                          @Param("team") teamId: String): Set<TeamMember>


}


