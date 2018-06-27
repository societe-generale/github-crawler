package com.societegenerale.githubcrawler.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.societegenerale.githubcrawler.RepositoryConfig
import com.societegenerale.githubcrawler.ownership.OwnershipParser
import feign.FeignException
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.regex.Pattern
import kotlin.collections.HashMap


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
                      val config: RepositoryConfig?,
                      @JsonIgnore
                      val reason: String?,
                      @JsonIgnore
                      val skipped: Boolean = false,
                      @JsonProperty("full_name")
                      val fullName: String,
                      @JsonIgnore
                      val indicators: Map<Branch, Map<String, String>> = HashMap(),
                      @JsonIgnore
                      val branchesToParse: List<Branch> = emptyList(),
                      @JsonIgnore
                      val tags: List<String> = emptyList(),
                      @JsonIgnore
                      val groups: List<String> = emptyList(),
                      @JsonIgnore
                      val crawlerRunId: String = "NO_CRAWLER_RUN_ID_DEFINED",
                      @JsonIgnore
                      val searchResults: Map<String, String> = HashMap(),
                      @JsonIgnore
                      var ownerTeam: String?
) {

    val log = LoggerFactory.getLogger(this.javaClass)

    fun flagAsExcludedIfRequired(repositoriesToExclude: List<String>): Repository {

        val matchedOnAnExclusionPattern = repositoriesToExclude.map { s -> Pattern.compile(s) }
                //below map returns true if it matches, false otherwise
                .map { p -> logIfMatches(p) }
                //we return the first item matching predicate "it", ie being true. Else, return false
                .firstOrNull { it } ?: false


        if (matchedOnAnExclusionPattern) {
            log.info("\texcluding repo {} because of server config ", name)

            return this.copy(excluded = true, reason = "excluded from server config side")
        }

        return this
    }

    private fun logIfMatches(p: Pattern): Boolean {

        if (p.matcher(name).find()) {
            log.debug("repo {} matched on exclusion pattern {}", name, p.pattern())
            return true
        }

        return false
    }


    fun getIndicatorsForBranch(branchName: String): Map<String, String> {

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

    fun copyTagsFromRepoConfig(): Repository {

        if (config != null) {
            return this.copy(tags = config.tags)
        } else {
            return this
        }
    }

    fun fetchOwner(ownershipParser: OwnershipParser) : Repository {
        val repositoryOwner = ownershipParser.computeOwnershipFor(name, 150)
        return this.copy(ownerTeam = repositoryOwner)
    }

    fun addGroups(groupsToSet: Array<out String>?): Repository {

        if (groupsToSet == null || groupsToSet.isEmpty()) {
            return this
        }

        return this.copy(groups = groupsToSet.asList())
    }

    class RepoConfigException : FeignException {

        constructor(message: String) : super(message)

        constructor(message: String, cause: Throwable) : super(message, cause)
    }

}
