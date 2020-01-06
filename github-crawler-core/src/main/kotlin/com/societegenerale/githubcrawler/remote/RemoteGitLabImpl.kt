package com.societegenerale.githubcrawler.remote

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import java.util.*
import java.util.stream.Collectors.toSet
import kotlin.collections.HashMap


/**
 * Couple of methods have special behavior, so can't use purely annotation based Feign impl.
 * Implementation is mainly based on Feign's Builder for standard calls, and OkHttpClient for the others
 *
 *
 */
@Suppress("TooManyFunctions") // most of methods are one liners, implementing the methods declared in interface
class RemoteGitLabImpl @JvmOverloads constructor(val gitLabUrl: String, val privateToken: String) : RemoteGitHub {

    companion object {
        const val REPO_LEVEL_CONFIG_FILE = ".gitlabCrawler"
    }

    private val internalGitLabClient: InternalGitLabClient = Feign.builder()
            .client(ApacheHttpClient())
            .encoder(GsonEncoder())
            .decoder(GitLabResponseDecoder())
            .errorDecoder(GiLabErrorDecoder())
            .decode404()
            .requestInterceptor(GitLabPrivateTokenSetter(privateToken))
            .logger(Slf4jLogger(RemoteGitLabImpl::class.java))
            .logLevel(Logger.Level.FULL)
            .target<InternalGitLabClient>(InternalGitLabClient::class.java, gitLabUrl)

    private val httpClient = OkHttpClient()

    // hack-ish way.. GitLab doesn't use repoName but repoId when retrieving repo details or files
    // so initializing a Map when discovering the repos for the first time, to reuse later
    private val repoNameToIdMapping = HashMap<String,Int>()

    val log = LoggerFactory.getLogger(this.javaClass)

    private val objectMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun fetchRepoConfig(repositoryFullName: String, defaultBranch: String): RepositoryConfig {

        val configFileOnRepository: String

        try {
            configFileOnRepository = internalGitLabClient.fetchFileOnRepo(repoNameToIdMapping[repositoryFullName]!!, defaultBranch, REPO_LEVEL_CONFIG_FILE)
        } catch (e: GitLabResponseDecoder.NoFileFoundFeignException) {
            return RepositoryConfig()
        }

        val decoder = GitLabResponseDecoder()

        return decoder.buildRepositoryConfig(configFileOnRepository)
    }


    override fun fetchRepoBranches(repositoryFullName: String): Set<Branch> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchCodeSearchResult(repository: Repository, query: String): SearchResult {
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
            log.warn("more than one GitLab group found for groupName $groupName : $gitLabGroups. Using the first one to perform the crawling. Refine your search criteria if that's not what you expect")
        }

        val gitLabGroup= gitLabGroups[0]

        val gitlabRepositories = internalGitLabClient.fetchRepositoriesForGroupId(gitLabGroup.id)

        return gitlabRepositories.stream().map { gitLabRepo -> recordMapping(gitLabRepo) }.map { gitLabRepo -> gitLabRepo!!.toRepository() }.collect(toSet())

    }

    private fun recordMapping(gitLabRepo: GitLabRepository?): GitLabRepository? {

        repoNameToIdMapping.put(gitLabRepo!!.path_with_namespace,gitLabRepo?.id)

        return gitLabRepo
    }

    override fun fetchFileContent(repositoryFullName: String, branchName: String, fileToFetch: String): String {

        try {
            return  internalGitLabClient.fetchFileOnRepo(repoNameToIdMapping.get(repositoryFullName)!!,branchName,fileToFetch)
        } catch (e: GitLabResponseDecoder.NoFileFoundFeignException) {
            //translating exception to a non Feign specific one
            throw NoFileFoundException("can't find $fileToFetch in repo $repositoryFullName, in branch $branchName")
        }

    }

    @Throws(NoReachableRepositories::class)
    override fun validateRemoteConfig(organizationName: String) {
        //TODO implement
    }

    override fun fetchOpenPRs(repositoryFullName: String): Set<PullRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class GitLabPrivateTokenSetter(val privateToken: String?) : RequestInterceptor {

    override fun apply(requestTemplate: RequestTemplate?) {

        if (requestTemplate != null && privateToken != null) {
            requestTemplate.header("PRIVATE-TOKEN", privateToken)
        }

    }

}


@Headers("Accept: application/json")
private interface InternalGitLabClient {

    @RequestLine("GET /groups?search={groupName}&sort=asc")
    fun fetchGroupByName(@Param("groupName") groupName: String): List<GitLabGroup>

    @RequestLine("GET /groups/{groupId}/projects")
    fun fetchRepositoriesForGroupId(@Param("groupId") groupId: Int): List<GitLabRepository>

    @RequestLine("GET /projects/{repoId}/repository/files/{filePath}/raw?ref={branchName}")
    fun fetchFileOnRepo(@Param("repoId") repositoryId: Int,
                        @Param("branchName") branchName: String,
                        @Param("filePath") fileToFetch: String): String


}

internal class GiLabErrorDecoder : ErrorDecoder {

    override fun decode(methodKey: String?, response: feign.Response?): java.lang.Exception {

        return errorStatus(methodKey, response);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class GitLabGroup(val id : Int,val name : String)

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

    @Throws(IOException::class)
    override fun decode(response: feign.Response, type: Type): Any {

        if (response.status() == HttpStatus.NOT_FOUND.value()) {

            if (type.typeName == String::class.java.name) {
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

    fun buildRepositoryConfig(responseAsString: String): RepositoryConfig {
        if (responseAsString.isEmpty()) {
            return RepositoryConfig()
        }

        try {
            return repoConfigMapper.readValue(responseAsString, RepositoryConfig::class.java)
        } catch (e: IOException) {
            throw Repository.RepoConfigException("unable to parse config for repo - content : \"$responseAsString\"", e)
        }
    }

    class NoFileFoundFeignException(message: String) : FeignException(message)

    class GitLabException(message: String) : FeignException(message)

}


