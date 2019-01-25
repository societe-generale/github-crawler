package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * This output will make a POST to the configured targetUrl, using the analyzed repository as a payload. If the analyzed repository has indicators for X branches, we'll post the indicators for each branch individually (so X times)
 *
 */
class HttpOutput(targetUrl: String,
                 private val restTemplate: RestTemplate,
                 private val currentLocalDate: LocalDate=LocalDate.now()) : GitHubCrawlerOutput {

    val log = LoggerFactory.getLogger(this.javaClass)

    private var internalTargetUrl = buildUrlAccordingToPlaceholderIfRequired(targetUrl)

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
                response = restTemplate.postForEntity(internalTargetUrl, output, String::class.java)

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

    /**
     * It's useful to be able to configure a targetURL with a date pattern, to push to an ElasticSearch index, for example http://myElasticSearch/{yyyy-MM}
     *
     * Since the "real" targetUrl is computed once at the beginning, if the crawler runs for too long it will send  data to an index that is "older" than the current time.
     * For example, if the crawler is configured to send data to http://myElasticSearch/{yyyy-MM-dd}, is launched at 11:58pm and runs for 5min, after midnight data will still be sent to D-1 index.
     *
     */
    private fun buildUrlAccordingToPlaceholderIfRequired(targetUrlFromProperties: String): String{

        return if(containsPlaceHolders(targetUrlFromProperties)){

            val patternToReplace=Regex(".*\\{(.*)}.*").find(targetUrlFromProperties)!!.groupValues[1]

            val valueForPlaceHolder=currentLocalDate.format(DateTimeFormatter.ofPattern(patternToReplace))

            return targetUrlFromProperties.replace("{$patternToReplace}",valueForPlaceHolder)
        }
        else{
            targetUrlFromProperties
        }

    }

    private fun containsPlaceHolders(targetUrlFromProperties: String): Boolean {
        return targetUrlFromProperties.matches(Regex(".*\\{.*}.*"))
    }

}
