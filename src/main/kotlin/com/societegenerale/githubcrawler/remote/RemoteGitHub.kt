package com.societegenerale.githubcrawler.remote

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.societegenerale.githubcrawler.RepositoryConfig
import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.model.SearchResult
import com.societegenerale.githubcrawler.model.commit.Commit
import com.societegenerale.githubcrawler.model.commit.DetailedCommit
import com.societegenerale.githubcrawler.model.team.Team
import com.societegenerale.githubcrawler.model.team.TeamMember
import feign.FeignException
import feign.Response
import feign.codec.Decoder
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


interface RemoteGitHub {

    fun fetchRepoConfig(repoFullName: String, defaultBranch: String): RepositoryConfig


    fun fetchRepoBranches(organizationName: String, repositoryName: String):
    //TODO this should be a Set, not a List
            List<Branch>

    fun fetchCodeSearchResult(repositoryFullName: String, query: String): SearchResult


    fun fetchFileContent(repositoryFullName: String, branchName: String, fileToFetch: String): String

    fun fetchCommits(organizationName: String,
                     repositoryFullName: String,
                     perPage: Int): Set<Commit>

    fun fetchCommit(organizationName: String,
                    repositoryFullName: String,
                    commitSha: String): DetailedCommit

    fun fetchTeams(token: String,
                   organizationName: String): Set<Team>

    fun fetchTeamsMembers(token: String,
                          teamId: String): Set<TeamMember>

    fun fetchRepositories(organizationName: String): Set<Repository>

}

internal class GitHubResponseDecoder : Decoder {
    val log = LoggerFactory.getLogger(this.javaClass)

    val mapper = ObjectMapper(YAMLFactory())

    init {
        mapper.registerModule(KotlinModule())
    }


    @Throws(IOException::class)
    override fun decode(response: Response, type: Type): Any {

        if (response.status() == HttpStatus.NOT_FOUND.value()) {

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

            else if (type.typeName == RepositoryConfig::class.java.name) {

                log.debug("\t ... as a specific " + RepositoryConfig::class.java.name)

                val responseAsString = extractResponseBodyAsString(response)

                return parseRepositoryConfigResponse(responseAsString, response)

            }

            log.debug("\t ... as a " + type.typeName)

            val jacksonConverter = MappingJackson2HttpMessageConverter(ObjectMapper().registerModule(KotlinModule()))
            val objectFactory = { HttpMessageConverters(jacksonConverter) }
            return ResponseEntityDecoder(SpringDecoder(objectFactory)).decode(response, type)

        }
    }

    private fun parseRepositoryConfigResponse(responseAsString: String, response: Response): Any {
        if (responseAsString.isEmpty()) {
            return RepositoryConfig()
        }

        try {
            return mapper.readValue(responseAsString, RepositoryConfig::class.java)
        } catch (e: IOException) {
            throw Repository.RepoConfigException("unable to parse config for repo - content : \"" + response.body() + "\"", e)
        }
    }

    private fun extractResponseBodyAsString(response: Response): String {

        val writer = StringWriter()
        IOUtils.copy(response.body().asInputStream(), writer, "UTF-8")

        return writer.toString()
    }

}


class NoRepoConfigFileFoundException(message: String) : FeignException(message)

