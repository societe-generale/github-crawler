package com.societegenerale.githubcrawler

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class GitHubCrawlerPropertiesTest {

  @Test
  fun cant_have_both_inclusion_and_exclusion_patterns_configured() {

    assertThatThrownBy {
      var invalidProperties = GitHubCrawlerProperties(
          repositoriesToInclude = listOf("pattern1"),
          repositoriesToExclude = listOf("pattern2")
      )
    }.isInstanceOf(IllegalStateException::class.java)

  }

}