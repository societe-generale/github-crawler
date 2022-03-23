package com.societegenerale.githubcrawler.config

import com.societegenerale.githubcrawler.GitHubCrawlerProperties
import com.societegenerale.githubcrawler.output.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.io.IOException

@Configuration
@EnableConfigurationProperties(GitHubCrawlerProperties::class)
open class GitHubCrawlerOutputConfig {

    val log = LoggerFactory.getLogger(this.javaClass)


    @Bean
    @ConditionalOnProperty(name = ["crawler.outputs.file.filenamePrefix"])
    @AutoConfigureOrder(value = 1)
    @Throws(IOException::class)
    open fun fileOutput(@Value("\${crawler.outputs.file.filenamePrefix:githubCrawlerOutput}") fileNamePrefix: String): GitHubCrawlerOutput {

        return FileOutput(fileNamePrefix)
    }

    @Bean
    @ConditionalOnProperty(name = ["crawler.outputs.http.targetUrl"])
    @AutoConfigureOrder(value = 2)
    open fun httpOutput(@Value("\${crawler.outputs.http.targetUrl:TARGET_URL_IS_MANDATORY}") targetUrl: String): GitHubCrawlerOutput {

        return HttpOutput(targetUrl, RestTemplate())
    }


    @Bean
    @ConditionalOnProperty(name = ["crawler.outputs.ciDroidCsvReadyFile.indicatorsToOutput"])
    @AutoConfigureOrder(value = 3)
    @Throws(IOException::class)
    open fun ciDroidReadyCsvFileOutput(@Value("\${crawler.outputs.ciDroidCsvReadyFile.indicatorsToOutput}") indicatorsToOutput: String): GitHubCrawlerOutput {

        return CIdroidReadyCsvFileOutput(indicatorsToOutput.split(","))
    }

    @Bean
    @ConditionalOnProperty(name = ["crawler.outputs.ciDroidJsonReadyFile.indicatorsToOutput"])
    @AutoConfigureOrder(value = 4)
    @Throws(IOException::class)
    open fun ciDroidReadyJsonFileOutput(@Value("\${crawler.outputs.ciDroidJsonReadyFile.indicatorsToOutput}") indicatorsToOutput: String,
                                        @Value("\${crawler.outputs.ciDroidJsonReadyFile.withTags:false}") withTags: Boolean): GitHubCrawlerOutput {

        return CIdroidReadyJsonFileOutput(indicatorsToOutput.split(","),withTags)
    }

    @Bean
    @ConditionalOnProperty(name = ["crawler.outputs.searchPatternInCodeCsvFileOutput.searchNameToOutput"])
    @AutoConfigureOrder(value = 5)
    @Throws(IOException::class)
    open fun searchPatternInCodeCsvFileOutput(@Value("\${crawler.outputs.searchPatternInCodeCsvFileOutput.searchNameToOutput}") searchNameToOutput: String): GitHubCrawlerOutput {

        return SearchPatternInCodeCsvFileOutput(searchNameToOutput)
    }

  @Bean
  @ConditionalOnProperty(name = ["crawler.outputs.recentRepositoriesCsvFileOutput"])
  @AutoConfigureOrder(value = 6)
  @Throws(IOException::class)
  open fun recentRepositoriesCsvFileOutput(): GitHubCrawlerOutput {

    return RecentRepositoriesCsvFileOutput()
  }



    @Bean
    @ConditionalOnMissingBean(GitHubCrawlerOutput::class)
    @AutoConfigureOrder(value = 100)
    open fun defaultOutput(): GitHubCrawlerOutput {

        log.warn("No bean of class " + GitHubCrawlerOutput::class.java + " found in config - there should be at least one under the 'output' property, so check your configuration. Configuring a default console output..")

        return ConsoleOutput()
    }


}
