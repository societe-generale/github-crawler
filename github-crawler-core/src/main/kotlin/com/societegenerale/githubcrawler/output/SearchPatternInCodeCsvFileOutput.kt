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

class SearchPatternInCodeCsvFileOutput @Throws(IOException::class)

constructor(private val searchNameToOutput: String) : GitHubCrawlerOutput {

    val log = LoggerFactory.getLogger(this.javaClass)

    private val finalOutputFileName: String

    companion object {
        const val PREFIX: String = "SearchPatternInCode_"
    }

    init {

        val now = LocalDateTime.now()

        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        finalOutputFileName = PREFIX + now.format(formatter) + ".txt"

        val writer = Files.newBufferedWriter(Paths.get(finalOutputFileName),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW)

        writer.write("repositoryFullName;location"+System.lineSeparator())

        writer.close()
    }

    @Throws(IOException::class)
    override fun output(analyzedRepository: Repository) {

        val writer = Files.newBufferedWriter(Paths.get(finalOutputFileName),
                StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND)

        try {

            val sb=StringBuilder()

            val searchItemPaths=analyzedRepository.searchResults.get(searchNameToOutput)

            if(searchItemPaths is List<*>){

                for(itemPathFound in searchItemPaths) {
                    sb.append(analyzedRepository.fullName).append(";")
                    sb.append(itemPathFound).append(";")
                    sb.append(System.lineSeparator());
                }

            }
            else {

                sb.append(analyzedRepository.fullName).append(";")
                sb.append("unable to parse item path").append(";")
                sb.append(System.lineSeparator())

            }
            writer.write(sb.toString())

        } catch (e: IOException) {
            log.error("problem while writing to output file", e)
        }

        writer.close()

    }
}
