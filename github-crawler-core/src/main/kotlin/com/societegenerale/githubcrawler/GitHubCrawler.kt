package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.output.GitHubCrawlerOutput
import com.societegenerale.githubcrawler.remote.RemoteSourceControl
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskToPerform
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors.toList

class GitHubCrawler(private val remoteSourceControl: RemoteSourceControl,
                    private val output: List<GitHubCrawlerOutput>,
                    private val repositoryEnricher: RepositoryEnricher,
                    private val gitHubCrawlerProperties: GitHubCrawlerProperties,
                    private val environment: Environment,
                    private val organizationName: String,
                    private val configValidator: ConfigValidator,
                    private val availableParsersAndTasks : AvailableParsersAndTasks) {

    companion object {
        const val NO_CRAWLER_RUN_ID_DEFINED: String = "NO_CRAWLER_RUN_ID_DEFINED"
    }

    val log = LoggerFactory.getLogger(this.javaClass)

    val tasksToPerform = ArrayList<RepoTaskToPerform>()

    @Throws(IOException::class)
    fun crawl() {

        val configValidationErrors = configValidator.getValidationErrors()

        if (configValidationErrors.isNotEmpty()) {
            throw IllegalStateException("There are some config validation errors - please double check the config. \n" + configValidationErrors.joinToString(separator = "\n", prefix = "\t - "))
        }

        gitHubCrawlerProperties.miscRepositoryTasks.forEach{

            val matchingTaskBuilder=availableParsersAndTasks.getRepoTasksBuilderByType(it.type)

            tasksToPerform.add(matchingTaskBuilder.buildTask(it.name, it.params))

        }

        val repositoriesFromOrga = remoteSourceControl.fetchRepositories(organizationName)

        fetchAndParseRepoContent(repositoriesFromOrga)

        output.stream().forEach { singleOutput ->
            try {
                singleOutput.finalizeOutput()
            } catch (e: IOException) {
                log.warn("problem while calling finalize on an output", e)
            }
        }

    }

    fun getGitHubCrawlerProperties(): GitHubCrawlerProperties {
        return gitHubCrawlerProperties
    }


    @Throws(IOException::class)
    private fun fetchAndParseRepoContent(repositoriesFromOrga: Set<Repository>) {

        log.info("active Spring profiles that we'll use as group attribute :")
        environment.activeProfiles.asIterable().forEach({ it -> log.info("- $it") })

        val crawlerRunId: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        log.info("crawler run ID : $crawlerRunId")

        log.info("${repositoriesFromOrga.size} repositories to crawl...")

        val repoStream = if (gitHubCrawlerProperties.crawlInParallel) repositoriesFromOrga.parallelStream() else repositoriesFromOrga.stream()

        repoStream.map { repo -> logRepoProcessing(repo) }
                .map { repo -> repo.flagAsExcludedIfRequired(gitHubCrawlerProperties) }
                .filter { repo -> shouldKeepForFurtherProcessing(repo, gitHubCrawlerProperties) }
                .map { repo -> repositoryEnricher.loadRepoSpecificConfigIfAny(repo) }
                .map { repo -> repo.flagAsExcludedIfConfiguredAtRepoLevel() }
                .filter { repo -> shouldKeepForFurtherProcessing(repo, gitHubCrawlerProperties) }
                .map { repo -> repo.copy(crawlerRunId = crawlerRunId) }
                .map { repo -> repo.copyTagsFromRepoTopics() }
                .map { repo -> repo.addGroups(environment.activeProfiles) }
                .map { repo -> repositoryEnricher.identifyBranchesToParse(repo, gitHubCrawlerProperties.crawlAllBranches) }
                .map { repo -> repositoryEnricher.fetchIndicatorsValues(repo, gitHubCrawlerProperties) }
                .map {repo -> repositoryEnricher.performMiscTasks(repo, tasksToPerform) }
                .map { repo -> publish(repo) }
                //calling collect to trigger the stream processing
                .collect(toList())

    }

    private fun logRepoProcessing(repo: Repository): Repository {
        log.info("processing repo ${repo.name}")
        return repo
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
