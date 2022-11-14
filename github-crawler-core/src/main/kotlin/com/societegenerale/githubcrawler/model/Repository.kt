package com.societegenerale.githubcrawler.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.RepositoryConfig
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.util.*
import java.util.regex.Pattern


data class Repository(val url: String,
                      val name: String,
                      @JsonProperty("default_branch")
                      val defaultBranch: String,
                      @JsonProperty("created_at")
                      val creationDate: Date,
                      @JsonProperty("updated_at")
                      val lastUpdateDate: Date,
                      @JsonIgnore
                      val excluded: Boolean = false,
                      @JsonIgnore
                      val config: RepositoryConfig? = null,
                      @JsonIgnore
                      val reason: String? = null,
                      @JsonIgnore
                      val skipped: Boolean = false,
                      @JsonProperty("full_name")
                      val fullName: String,
                      @JsonIgnore
                      val indicators: Map<Branch, Map<String, Any>> = HashMap(),
                      @JsonIgnore
                      val branchesToParse: Set<Branch> = emptySet(),
                      @JsonIgnore
                      val tags: List<String> = emptyList(),
                      @JsonIgnore
                      val groups: List<String> = emptyList(),
                      @JsonIgnore
                      val crawlerRunId: String = "NO_CRAWLER_RUN_ID_DEFINED",
                      @JsonIgnore
                      var miscTasksResults: Map<Branch, Map<String, Any>> = HashMap(),
                      @JsonIgnore
                      var topics: List<String> = emptyList()
) {

    val log = LoggerFactory.getLogger(this.javaClass)

    fun flagAsExcludedIfRequired(config : GitHubCrawlerProperties): Repository {

      val repositoriesToExclude = config.repositoriesToExclude
      val repositoriesToInclude = config.repositoriesToInclude

      if(repositoriesToExclude.isNotEmpty()) {
        val matchedOnAnExclusionPattern = repoNameMatchesAnyPatternsFrom(repositoriesToExclude)

        if (matchedOnAnExclusionPattern) {
          log.info("\texcluding repo ${name} because of server config ")

          return this.copy(excluded = true, reason = "excluded from server config side")
        }
      }
      else if(repositoriesToInclude.isNotEmpty()){
        val matchedOnAnInclusionPattern = repoNameMatchesAnyPatternsFrom(repositoriesToInclude)

        if (!matchedOnAnInclusionPattern) {
          log.info("\texcluding repo ${name} because of server config - not matching the inclusion pattern")

          return this.copy(excluded = true, reason = "excluded from server config side")
        }

      }

      return this
    }

  private fun repoNameMatchesAnyPatternsFrom(repositoriesToInclude: List<String>) = repositoriesToInclude.map { s -> Pattern.compile(s) }
      //below map returns true if it matches, false otherwise
      .map { p -> logIfMatches(p) }
      //we return the first item matching predicate "it", ie being true. Else, return false
      .firstOrNull { it } ?: false

  private fun logIfMatches(p: Pattern): Boolean {

        if (p.matcher(name).find()) {
            log.debug("repo {} matched on exclusion/inclusion pattern {}", name, p.pattern())
            return true
        }

        return false
    }


    fun getIndicatorsForBranch(branchName: String): Map<String, Any> {

        return indicators.get(Branch(branchName)) ?: emptyMap()

    }

    fun flagAsExcludedIfConfiguredAtRepoLevel(): Repository {

        if (excluded) {
            return this
        }

        if (config != null && config.excluded) {
            return this.copy(
                    excluded = true,
                    reason = "excluded from repo config side"
            )
        }

        return this

    }

    fun copyTagsFromRepoTopics(): Repository {
        return this.copy(tags = this.topics)
    }

    fun addGroups(groupsToSet: Array<out String>?): Repository {

        if (groupsToSet == null || groupsToSet.isEmpty()) {
            return this
        }

        return this.copy(groups = groupsToSet.asList())
    }

    class RepoConfigException : FeignException {

        constructor(statusCode: HttpStatus, message: String) : super(statusCode.value(),message)

        constructor(statusCode:HttpStatus, message: String, cause: Throwable) : super(statusCode.value(),message, cause)
    }

}
