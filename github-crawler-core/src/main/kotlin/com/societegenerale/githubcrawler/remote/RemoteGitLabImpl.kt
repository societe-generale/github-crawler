package com.societegenerale.githubcrawler.remote

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors.toList
import java.util.stream.Collectors.toSet


/**
 * Couple of methods have special behavior, so can't use purely annotation based Feign impl.
 * Implementation is mainly based on Feign's Builder for standard calls, and OkHttpClient for the others
 *
 *
 */
@Suppress("TooManyFunctions") // most of methods are one liners, implementing the methods declared in interface
class RemoteGitLabImpl @JvmOverloads constructor(val gitLabUrl: String, val usersReposInsteadOfOrgasRepos: Boolean = false, val oauthToken: String) : RemoteGitHub {



    companion object {
        const val REPO_LEVEL_CONFIG_FILE = ".githubCrawler"
        const val APPLICATION_JSON = "application/json"
        const val ACCEPT = "accept"
        const val CONFIG_VALIDATION_REQUEST_HEADER = "X-configValidationRequest"
        const val APPLICATION_GITHUB_MERCY_PREVIEW_JSON = "application/vnd.github.mercy-preview+json"
    }

    private val internalGitLabClient: InternalGitLabClient = Feign.builder()
            .client(ApacheHttpClient())
            .encoder(GsonEncoder())
            .decoder(GitLabResponseDecoder())
            .errorDecoder(GiLabErrorDecoder())
            .decode404()
            .requestInterceptor(GitLabOauthTokenSetter(oauthToken))
            .logger(Slf4jLogger(RemoteGitLabImpl::class.java))
            .logLevel(Logger.Level.FULL)
            .target<InternalGitLabClient>(InternalGitLabClient::class.java, gitLabUrl)

    private val httpClient = OkHttpClient()


    val log = LoggerFactory.getLogger(this.javaClass)

    private val objectMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    override fun fetchRepoConfig(repositoryFullName: String, defaultBranch: String): RepositoryConfig {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchRepoBranches(repositoryFullName: String): Set<Branch> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchCodeSearchResult(repository: Repository, query: String): SearchResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchFileContent(repositoryFullName: String, branchName: String, fileToFetch: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchCommits(repositoryFullName: String, perPage: Int): Set<Commit> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchCommit(repositoryFullName: String, commitSha: String): DetailedCommit {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchTeams(organizationName: String): Set<Team> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchTeamsMembers(teamId: String): Set<TeamMember> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchRepositories(groupName: String): Set<Repository> {

        val gitLabGroups = internalGitLabClient.fetchGroupByName(groupName)

        if(gitLabGroups.isEmpty() ){
            throw GitLabResponseDecoder.GitLabException("no GitLab group found for groupName $groupName, so can't find any repositories to crawl")
        }

        if(gitLabGroups.size > 1 ){
            throw GitLabResponseDecoder.GitLabException("more than one GitLab group found for groupName $groupName : $gitLabGroups. PLease refine the groupName so that the search yields only one result")
        }

        val gitLabGroup= gitLabGroups[0]

        val gitlabRepositories = internalGitLabClient.fetchRepositoriesForGroupId(gitLabGroup.id)

        return gitlabRepositories.stream().map { gitLabRepo -> gitLabRepo.toRepository() }.collect(toSet())

    }

    @Throws(NoReachableRepositories::class)
    override fun validateRemoteConfig(organizationName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchOpenPRs(repositoryFullName: String): Set<PullRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private fun addOAuthTokenIfRequired(requestBuilder: okhttp3.Request.Builder): Unit {

        if (oauthToken.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "token " + oauthToken)
        }

    }

}

class GitLabOauthTokenSetter(val oauthToken: String?) : RequestInterceptor {

    override fun apply(requestTemplate: RequestTemplate?) {

        if (requestTemplate != null && oauthToken != null) {
            requestTemplate.header("Authorization", "token " + oauthToken)
        }

    }

}


@Headers("Accept: application/json")
private interface InternalGitLabClient {



    @RequestLine("GET /groups?search={groupName}")
    fun fetchGroupByName(@Param("groupName") groupName: String): List<GitLabGroup>

    @RequestLine("GET /groups/{groupId}/projects")
    fun fetchRepositoriesForGroupId(@Param("groupId") groupId: Int): List<GitLabRepository>




}

internal class GiLabErrorDecoder : ErrorDecoder {

    override fun decode(methodKey: String?, response: feign.Response?): java.lang.Exception {

        if (response?.status() == HttpStatus.CONFLICT.value()) {
            throw GitLabResponseDecoder.GitLabException("problem while fetching content... conflict state as per HTTP 409 code")
        }

        return errorStatus(methodKey, response);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitLabGroup(val id : Int,val name : String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitLabRepository (val id : Int,val web_url : String, val path : String, val path_with_namespace : String, val default_branch : String, val created_at : Date, val last_activity_at : Date, val tag_list : List<String>) {

    fun toRepository(): Repository {

        return  Repository(url = web_url,
                           fullName = path_with_namespace,
                           name = path,
                           defaultBranch = default_branch,
                           creationDate = created_at,
                           lastUpdateDate = last_activity_at,
                           topics = tag_list)

    }
}

internal class GitLabResponseDecoder : Decoder {
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

    class GitLabException(message: String) : FeignException(message)

}


