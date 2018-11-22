package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.Logger
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Abstract class for csv file outputs - taking care of creating the file, writing the header, and closing at the end.
 */
abstract class CsvFileOutput(initParam : Any) : GitHubCrawlerOutput{

    abstract fun getCsvHeaderFrom(initParam : Any):String

    abstract fun getPrefix():String

    abstract fun outputRepository(analyzedRepository: Repository):StringBuilder

    abstract val log : Logger

    val finalOutputFileName: String

    init {

        val now = LocalDateTime.now()

        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        finalOutputFileName = getPrefix() + now.format(formatter) + ".csv"

        val writer = Files.newBufferedWriter(Paths.get(finalOutputFileName),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW)

        writer.write(getCsvHeaderFrom(initParam)+System.lineSeparator())

        writer.close()
    }

    @Throws(IOException::class)
    override fun output(analyzedRepository: Repository) {

        val writer = Files.newBufferedWriter(Paths.get(finalOutputFileName),
                StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND)

        try {

             writer.write(outputRepository(analyzedRepository).toString())

        } catch (e: IOException) {
            log.error("problem while writing to output file", e)
        }

        writer.close()
    }
}