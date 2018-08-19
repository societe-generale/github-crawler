package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.model.SearchResult
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import com.societegenerale.githubcrawler.ownership.OwnershipParser
import com.societegenerale.githubcrawler.parsers.FileContentParser
import com.societegenerale.githubcrawler.parsers.SearchResultParser
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors.toList

class GitHubCrawler(private val remoteGitHub: RemoteGitHub,
                    private val ownershipParser: OwnershipParser,
                    private val output: List<GitHubCrawlerOutput>,
                    private val repositoryEnricher: RepositoryEnricher,
                    private val gitHubCrawlerProperties: GitHubCrawlerProperties,
                    private val environment: Environment,
                    private val organizationName: String,
                    private val gitHubUrl: String,
                    private val configValidator: ConfigValidator) {

    companion object{
        const val NO_CRAWLER_RUN_ID_DEFINED : String = "NO_CRAWLER_RUN_ID_DEFINED"
        val availableFileContentParsers = HashMap<String, FileContentParser>()
        val availableSearchResultParsers = HashMap<String, SearchResultParser>()
    }

    val log = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    private val fileContentParsers: List<FileContentParser> = emptyList()

    @Autowired
    private val searchResultParsers: List<SearchResultParser> = emptyList()

    @Throws(IOException::class)
    fun crawl() {

        val configValidationErrors = configValidator.getValidationErrors()

        if(configValidationErrors.isNotEmpty()){
            throw IllegalStateException("There are some config validation errors - please double check the config. \n"+configValidationErrors.joinToString(separator = "\n", prefix = "\t - ") )
        }

        if (availableFileContentParsers.isEmpty() || searchResultParsers.isEmpty()) {
            initParsersConfig()
        }

        var repositoriesFromOrga = remoteGitHub.fetchRepositories(organizationName)

        fetchAndParseRepoContent(repositoriesFromOrga)

        output.stream().forEach { singleOutput ->
            try {
                singleOutput.finalizeOutput()
            } catch (e: IOException) {
                log.warn("problem while calling finalize on an output", e)
            }
        }

    }

    fun getGitHubCrawlerProperties() : GitHubCrawlerProperties{
        return gitHubCrawlerProperties
    }

    private fun initParsersConfig() {

        //TODO validate we don't have 2 parsers with same nameInConfig

        //TODO check that all methods in gitHubCrawlerProperties are known, ie mappable to a Parser.
        // Otherwise, we'll get an NPE later during processing

        fileContentParsers.forEach {
            log.info("adding ${it.getNameInConfig()} in the map of available file parsers..")
            availableFileContentParsers.put(it.getNameInConfig(), it)
        }
        log.info("--> ${availableFileContentParsers.size} available parser(s)..")

        //TODO fail fast if availableFileContentParsers is empty

        searchResultParsers.forEach {
            log.info("adding ${it.getNameInConfig()} in the map of available search result parsers..")
            availableSearchResultParsers.put(it.getNameInConfig(), it)
        }
        log.info("--> ${availableSearchResultParsers.size} available parser(s)..")

    }

    @Throws(IOException::class)
    private fun fetchAndParseRepoContent(repositoriesFromOrga: Set<Repository>) {

        //TODO control better the number of threads

        log.info("active Spring profiles that we'll use as group attribute :")
        environment.activeProfiles.asIterable().forEach({it ->  log.info("- $it")})

        val crawlerRunId : String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        log.info("crawler run ID : $crawlerRunId")

        repositoriesFromOrga.parallelStream()
                .map { repo -> repo.flagAsExcludedIfRequired(gitHubCrawlerProperties.repositoriesToExclude) }
                .filter { repo -> shouldKeepForFurtherProcessing(repo, gitHubCrawlerProperties) }
                .map { repo -> repositoryEnricher.loadRepoSpecificConfigIfAny(repo) }
                .map { repo -> repo.flagAsExcludedIfConfiguredAtRepoLevel() }
                .filter { repo -> shouldKeepForFurtherProcessing(repo, gitHubCrawlerProperties) }
                .map{repo -> repo.copy(crawlerRunId= crawlerRunId)}
                .map { repo -> repo.copyTagsFromRepoTopics() }
                .map { repo -> repo.addGroups(environment.activeProfiles) }
                .map { repo -> repositoryEnricher.identifyBranchesToParse(repo,gitHubCrawlerProperties.crawlAllBranches, organizationName!!) }
                .map { repo -> repositoryEnricher.fetchIndicatorsValues(repo,gitHubCrawlerProperties) }
                .map { repo -> repo.fetchOwner(ownershipParser) }
                .map { repo -> applySearchResultsOnRepo(gitHubCrawlerProperties, repo) }
                .map { repo -> publish(repo) }
                //calling collect to trigger the stream processing
                .collect(toList())

    }

    /**
     * Not able to implement the search call to GitHub using Feignclient, as the parameters follow a not very standard pattern.
     * So implementing the call with a regular restTemplate
     */
    private fun applySearchResultsOnRepo(gitHubCrawlerProperties: GitHubCrawlerProperties, repo: Repository): Repository {

        val searchResults=gitHubCrawlerProperties.searchesPerRepo.asIterable()
                       .map{ (searchName,searchParam) -> Triple(searchName,searchParam,fetchCodeSearchResult(repo,searchParam.queryString)) }
                       .map{ (searchName,searchParam,searchResult) -> Pair(searchName,parseSearchResult(searchParam,searchResult))}
                       .toMap()

        return repo.copy(searchResults=searchResults)
    }

    private fun parseSearchResult(searchParam: SearchParam, searchResult: SearchResult): String {

        val parser= availableSearchResultParsers.get(searchParam.method);

        return parser!!.parse(searchResult)
    }

    private fun fetchCodeSearchResult(repo: Repository, queryString: String): SearchResult {

        val restTemplate = RestTemplate()

        val responseEntity = restTemplate.exchange("$gitHubUrl/search/code?" + buildQueryString(queryString, repo),
                HttpMethod.GET, null, object : ParameterizedTypeReference<SearchResult>() {

        })

        if(responseEntity.hasBody()){
            return responseEntity.body
        }
        else{
            //TOOD return empty searchResult
            return SearchResult(0)
        }

    }

    private fun buildQueryString(queryString: String, repo: Repository): String {

        return "q=$queryString repo:${repo.fullName}"

    }

    private fun publish(repo: Repository): Repository {

        output.stream().forEach { singleOutput ->
            try {
                singleOutput.output(repo)
            } catch (e: IOException) {
                log.warn("unable to publish results for repo $repo in output $output", e)
            }
        }

        return repo
    }

    private fun shouldKeepForFurtherProcessing(repo: Repository, gitHubCrawlerProperties: GitHubCrawlerProperties): Boolean {

        if (gitHubCrawlerProperties.publishExcludedRepositories) {
            return true
        }

        if (repo.excluded) {
            log.info("filtering out repo {} from further processing", repo.name)
            return false
        }

        return true
    }

}
