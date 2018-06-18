package com.societegenerale.githubcrawler


import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import com.societegenerale.githubcrawler.remote.NoFileFoundException
import com.societegenerale.githubcrawler.remote.RemoteGitHub
import org.slf4j.LoggerFactory


class RepositoryEnricher(val remoteGitHub: RemoteGitHub){

    companion object {
        const val REPO_LEVEL_CONFIG_FILE = ".githubCrawler"
    }


    val log = LoggerFactory.getLogger(this.javaClass)

    fun identifyBranchesToParse(repository: Repository, crawlAllBranches: Boolean, orgaName: String): Repository {

        if (crawlAllBranches) {

            val branchesFound: List<Branch> = remoteGitHub.fetchRepoBranches(orgaName, repository.name);

            val repoWithBranches = repository.copy(branchesToParse = branchesFound)

            return repoWithBranches
        } else {
            return repository.copy(branchesToParse = listOf(Branch(repository.defaultBranch)))
        }
    }

    fun loadRepoSpecificConfigIfAny(repository: Repository): Repository {

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

        if (repository.excluded) {
            return repository
        }

        val foundIndicators: MutableMap<Branch, Map<String, String>> = mutableMapOf()

        for (branch in repository.branchesToParse) {

            val foundIndicatorsForBranch =
                    gitHubCrawlerPropertiesByFile.indicatorsToFetchByFile.map { (fileToParse, indicatorsToFetch) -> fetchFileAndParseIndicatorsFromIt(repository,branch, fileToParse, indicatorsToFetch) }
                            .reduce { acc, map -> acc + map }

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


        val fileContent: String?
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
                .map { GitHubCrawler.availableFileContentParsers.get(it.method)!!.parseFileContentForIndicator(fileContent, pathToFileToGetIndicatorsFrom, it) }
                .reduce { acc, item -> acc + item }

    }

























}