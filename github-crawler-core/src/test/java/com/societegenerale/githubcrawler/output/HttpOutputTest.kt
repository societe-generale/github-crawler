package com.societegenerale.githubcrawler.output


import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.*


class HttpOutputTest {

    private val logger = LoggerFactory.getLogger(HttpOutput::class.java!!)


    @Test
    fun shouldLogResponseBodyWhenErrorDuringPost() {
        //Mock logging infra
        val root = LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        val mockAppender: Appender<ILoggingEvent> = mock()

        `when`(mockAppender.name).thenReturn("MOCK")
        root.addAppender(mockAppender)


        val mockRestTemplate: RestTemplate = mock()

        val httpOutput = HttpOutput("http://localhost", mockRestTemplate)

        `when`(mockRestTemplate.postForEntity(anyString(),
                                              any(OutputIndicator::class.java),
                                              eq(String::class.java)
                                             )
              ).thenThrow(HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request details"))

        val dummyRepo = Repository(name = "repo1",
                creationDate = Date(),
                config = null,
                defaultBranch = "master",
                fullName = "orgName/repoName1",
                lastUpdateDate = Date(),
                ownerTeam = null,
                reason = null,
                url = "http://hello",
                branchesToParse = listOf(Branch("master"))
        )

        //ACTION
        httpOutput.output(dummyRepo)

        argumentCaptor<ILoggingEvent>().apply {
            verify(mockAppender).doAppend(capture())

            val matchingLogEvent = allValues.firstOrNull{ logEvent -> logEvent.formattedMessage.contains("Bad Request details") }

            assertThat(matchingLogEvent ).`as`("can't find a log statement with expected message").isNotNull
        }
    }
}

