package com.societegenerale.githubcrawler.output

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.util.*


class HttpOutputTest {

  private val logger = LoggerFactory.getLogger(HttpOutput::class.java)

  private val dummyRepo = Repository(name = "repo1",
      creationDate = Date(),
      config = null,
      defaultBranch = "master",
      fullName = "orgName/repoName1",
      lastUpdateDate = Date(),
      reason = null,
      url = "http://hello",
      branchesToParse = setOf(Branch("master"))
  )

  private val mockRestTemplate = mock(RestTemplate::class.java)

  inline fun <reified T : Any> argumentCaptor() = ArgumentCaptor.forClass(T::class.java)

  @Test
  fun shouldLogResponseBodyWhenErrorDuringPost() {
    //Mock logging infra
    val root = LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
    val mockAppender = mock(Appender::class.java)

    `when`(mockAppender.name).thenReturn("MOCK")
    root.addAppender(mockAppender as Appender<ILoggingEvent>?)

    val httpOutput = HttpOutput("http://localhost", mockRestTemplate)

    `when`(mockRestTemplate.postForEntity(any<String>(),
        any<OutputIndicator>(),
        eq(String::class.java)
    )
    ).thenThrow(HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request details"))

    //ACTION
    httpOutput.output(dummyRepo)

    val captor = argumentCaptor<ILoggingEvent>()

    verify(mockAppender).doAppend(captor.capture())

    val matchingLogEvent = captor.allValues.firstOrNull { logEvent -> logEvent.formattedMessage.contains("Bad Request details") }

    assertThat(matchingLogEvent).`as`("can't find a log statement with expected message").isNotNull

  }

  @Test
  fun shouldHitProperUrlWhenPlaceHolderProvided() {

    val httpOutput = HttpOutput("http://localhost/{YYYY-MM}", mockRestTemplate, LocalDate.of(2019, 1, 25))

    `when`(mockRestTemplate.postForEntity(any<String>(),
        any<OutputIndicator>(),
        eq(String::class.java))
    ).thenReturn(ResponseEntity("content", HttpStatus.ACCEPTED))

    //ACTION
    httpOutput.output(dummyRepo)

    verify(mockRestTemplate, times(1)).postForEntity(eq("http://localhost/2019-01"),
        any<OutputIndicator>(),
        eq(String::class.java)
    )
  }

  @Test
  fun shouldHitProperUrlWhen_no_placeHolderProvided() {

    val httpOutput = HttpOutput("http://localhost/YYYY-MM", mockRestTemplate, LocalDate.of(2019, 1, 25))

    `when`(mockRestTemplate.postForEntity(any<String>(),
        any<OutputIndicator>(),
        eq(String::class.java))
    ).thenReturn(ResponseEntity("content", HttpStatus.ACCEPTED))

    //ACTION
    httpOutput.output(dummyRepo)

    verify(mockRestTemplate, times(1)).postForEntity(eq("http://localhost/YYYY-MM"),
        any<OutputIndicator>(),
        eq(String::class.java)
    )
  }

}

