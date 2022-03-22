package com.societegenerale.githubcrawler


import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.NoFileFoundException
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskToPerform
import org.slf4j.LoggerFactory


class RepositoryEnricher(val remoteGitHub: RemoteGitHub,
                         val availableParsersAndTasks : AvailableParsersAndTasks = AvailableParsersAndTasks(emptyList(), emptyList())){

    val log = LoggerFactory.getLogger(this.javaClass)

    fun identifyBranchesToParse(repository: Repository, crawlAllBranches: Boolean): Repository {

        if (crawlAllBranches) {

            val branchesFound = remoteGitHub.fetchRepoBranches(repository.fullName);

            val repoWithBranches = repository.copy(branchesToParse = branchesFound)

            return repoWithBranches
        } else {
            return repository.copy(branchesToParse = setOf(Branch(repository.defaultBranch)))
        }
    }

    fun loadRepoSpecificConfigIfAny(repository: Repository): Repository {

        log.info("loadRepoSpecificConfigIfAny from "+this)

        if (repository.excluded) {
            return repository
        }

        val repositoryConfig: RepositoryConfig

        try {
            log.debug("loading repo config for $repository.fullName")

            repositoryConfig = remoteGitHub.fetchRepoConfig(repository.fullName,repository.defaultBranch)

            log.debug("..repo config found ${repositoryConfig.toString()}")
        } catch (e: NoFileFoundException) {

            return repository

        } catch (e: Repository.RepoConfigException) {

            log.warn("problem while parsing repo config for $repository.fullName --> skipping", e)

            return repository.copy(skipped = true, reason = e.message)
        }

        return repository.copy(config = repositoryConfig)
    }



    fun fetchIndicatorsValues(repository: Repository,gitHubCrawlerPropertiesByFile: GitHubCrawlerProperties): Repository {

        log.info("fetchIndicatorsValues from "+this)

        //TODO this check should be removed
        if (repository.excluded) {
            return repository
        }

        val foundIndicators: MutableMap<Branch, Map<String, String>> = mutableMapOf()

        for (branch in repository.branchesToParse) {

            val foundIndicatorsForBranch =
                    gitHubCrawlerPropertiesByFile.indicatorsToFetchByFile.map { (fileToParse, indicatorsToFetch) -> fetchFileAndParseIndicatorsFromIt(repository,branch, fileToParse, indicatorsToFetch) }
                            .fold(mapOf<String,String>()){ acc, map -> acc + map }

            foundIndicators.put(branch, foundIndicatorsForBranch)

        }

        return repository.copy(indicators = foundIndicators)

    }

    private fun fetchFileAndParseIndicatorsFromIt(repository: Repository, branch: Branch, fileToFetchAndProcess: com.societegenerale.githubcrawler.FileToParse, indicatorsToFetch: List<IndicatorDefinition>): Map<String, String> {

        log.debug("fetching ${fileToFetchAndProcess.name} for repo $repository.fullName..")

        val fileRedirectedPath = repository.config?.filesToParse?.filter { it.name == fileToFetchAndProcess.name }?.map { it.redirectTo }?.firstOrNull()

        var pathToFileToGetIndicatorsFrom: String

        if (fileRedirectedPath != null) {
            log.info("redirection found for ${fileToFetchAndProcess.name} : $fileRedirectedPath")
            pathToFileToGetIndicatorsFrom=fileRedirectedPath
        } else {
            log.debug("no redirection found for ${fileToFetchAndProcess.name}")
            pathToFileToGetIndicatorsFrom=fileToFetchAndProcess.name
        }


        val fileContent: String
        try {
            fileContent = fetchFileWithIndicatorsToFind(repository.fullName,branch, pathToFileToGetIndicatorsFrom)
        } catch (e: NoFileFoundException) {
            return emptyMap()
        }

        return parseIndicatorsFromFileContent(fileContent, pathToFileToGetIndicatorsFrom,indicatorsToFetch)
    }


    private fun fetchFileWithIndicatorsToFind(repoFullName : String, branch: Branch, fileToFetchAndProcess: String): String {
            return remoteGitHub.fetchFileContent(repoFullName, branch.name, fileToFetchAndProcess)
    }


    private fun parseIndicatorsFromFileContent(fileContent: String, pathToFileToGetIndicatorsFrom:String, indicatorsToFetch: List<IndicatorDefinition>): Map<String, String> {

        return indicatorsToFetch.asSequence()
                .map { availableParsersAndTasks.getParserByName(it.type).parseFileContentForIndicator(fileContent, pathToFileToGetIndicatorsFrom, it) }
                .reduce { acc, item -> acc + item }

    }

    fun performMiscTasks(repo: Repository, miscRepositoryTasks: List<RepoTaskToPerform>): Repository {

        log.info("performMiscTasks "+miscRepositoryTasks.size+" from "+this)

        val allActionsResults=miscRepositoryTasks.map { task -> task.perform(repo) } //executing all tasks -> getting a List<Map<Branch,Pair<String, Any>>>

                                                  .asSequence().flatMap {it.asSequence()}
                                                  .groupBy({ it.key }, { it.value }) //now having a Map<Branch,List<Pair<String, Any>>>

                                                  .mapValues{it.value.toMap()}// mapping the values, to get a Map<String,Any> of results associated to a given Branch

        return repo.copy(miscTasksResults = allActionsResults)
    }



}