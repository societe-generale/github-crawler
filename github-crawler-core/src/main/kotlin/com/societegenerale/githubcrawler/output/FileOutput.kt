package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FileOutput @Throws(IOException::class)
constructor(filenamePrefix: String) : GitHubCrawlerOutput {

    val log = LoggerFactory.getLogger(this.javaClass)

    private val finalOutputFileName: String

    init {

        val now = LocalDateTime.now()

        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        finalOutputFileName = filenamePrefix + "_" + now.format(formatter) + ".txt"

        Files.newBufferedWriter(Paths.get(finalOutputFileName),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW).use{

            it.write("OUTPUT FOR GitHub crawler - " + now.format(formatter) + System.lineSeparator())

        }

    }

    @Throws(IOException::class)
    override fun output(analyzedRepository: Repository) {

        Files.newBufferedWriter(Paths.get(finalOutputFileName),
                StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND).use{

            it.write(analyzedRepository.toString()+ System.lineSeparator())
        }
    }
}
