package com.societegenerale.githubcrawler.output

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class OutputIndicator(val name: String,
                           val branchName: String,
                           val creationDate: Date,
                           val lastUpdateDate: Date,
                           val indicators: Map<String, Any> = HashMap(),
                           val tags: List<String> = ArrayList(),
                           val groups: List<String> = ArrayList(),
                           val crawlerRunId: String,
                           val miscTasksResults: Map<String,Any> = HashMap()) {

    val timestamp: String = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)

}