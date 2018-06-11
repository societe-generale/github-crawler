package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestTemplate


class HttpOutput(private val targetUrl: String) : GitHubCrawlerOutput {

    val log = LoggerFactory.getLogger(this.javaClass)

    private val restTemplate: RestTemplate

    init {
        this.restTemplate = RestTemplate()
    }

    override fun output(analyzedRepository: Repository) {

        for (branch in analyzedRepository.branchesToParse) {

            val output = OutputIndicator(analyzedRepository.name,
                    branch.name,
                    analyzedRepository.creationDate,
                    analyzedRepository.lastUpdateDate,
                    analyzedRepository.indicators[branch] ?: emptyMap(),
                    analyzedRepository.tags,
                    analyzedRepository.groups,
                    analyzedRepository.crawlerRunId,
                    analyzedRepository.searchResults,
                    analyzedRepository.ownerTeam ?: "Undefined")

            val response = restTemplate.postForEntity(targetUrl, output, String::class.java)

            if (!response.statusCode.is2xxSuccessful) {
                log.warn("couldn't push result for repo {} - code {} - {}", analyzedRepository.name, response.statusCode, response.body)
            }

        }
    }

}
