package com.societegenerale.githubcrawler.output

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class OutputIndicator(val name: String,
                           val branchName: String,
                           @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
                           val creationDate: Date,
                           @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
                           val lastUpdateDate: Date,
                           val indicators: Map<String, Any> = HashMap(),
                           val tags: List<String> = ArrayList(),
                           val groups: List<String> = ArrayList(),
                           val crawlerRunId: String,
                           val miscTasksResults: Map<String,Any> = HashMap()) {

    val timestamp: String = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)

}