package com.societegenerale.githubcrawler.model

import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.sql.Date

class RepositoryTest{

  @Test
  fun repo_flagged_according_to_inclusion_config(){

    val inclusionConfigured = GitHubCrawlerProperties(
        repositoriesToInclude = listOf("prefix-.*-service")
    )

    val repoTemplate=Repository(name="SOME_NAME_TO_REPLACE",
        url="someUrl",
        defaultBranch = "main",
        creationDate = Date.valueOf("2022-11-14"),
        lastUpdateDate = Date.valueOf("2022-11-14"),
        fullName = "someFullName")


    val repoToInclude=repoTemplate.copy(name="prefix-should_keep-service")
    assertThat(repoToInclude.flagAsExcludedIfRequired(inclusionConfigured).excluded)
        .`as`("repo should NOT be excluded")
        .isFalse


    val repoToNotInclude=repoTemplate.copy(name="prefix-should_not_keep-ui")
    assertThat(repoToNotInclude.flagAsExcludedIfRequired(inclusionConfigured).excluded)
        .`as`("repo SHOULD be excluded")
        .isTrue
  }


}