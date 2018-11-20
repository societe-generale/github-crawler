package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate


class HttpOutput(private val targetUrl: String,
                 private val restTemplate: RestTemplate) : GitHubCrawlerOutput {

    val log = LoggerFactory.getLogger(this.javaClass)

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
                    analyzedRepository.miscTasksResults[branch] ?: emptyMap())

            val response : ResponseEntity<String>

            try {
                response = restTemplate.postForEntity(targetUrl, output, String::class.java)

                if (!response.statusCode.is2xxSuccessful) {
                    logHttpError(analyzedRepository.name, response.statusCodeValue,response.body)
                }
            } catch (e : HttpClientErrorException) {
                logHttpError(analyzedRepository.name, e.rawStatusCode,e.message ?: "no additional message provided")
            }
        }
    }

    private fun logHttpError(repoName: String, errorCode : Int, message : String?) {
        log.warn("couldn't push result for repo {} - code {} - {}", repoName, errorCode, message)
    }

}
