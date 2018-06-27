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

class CIdroidReadyCsvFileOutput @Throws(IOException::class)

constructor(val indicatorsToOutput: List<String>) : GitHubCrawlerOutput {

    val log = LoggerFactory.getLogger(this.javaClass)

    private val finalOutputFileName: String

    init {

        val now = LocalDateTime.now()

        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        finalOutputFileName = "CIdroidReadyContent_" + now.format(formatter) + ".txt"

        val writer = Files.newBufferedWriter(Paths.get(finalOutputFileName),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW)

        writer.write("repositoryFullName;branchName;filePath\n")

        writer.close()
    }

    @Throws(IOException::class)
    override fun output(analyzedRepository: Repository) {

        val writer = Files.newBufferedWriter(Paths.get(finalOutputFileName),
                StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND)

        try {

            val sb=StringBuilder()

            for((branch,actualIndicators) in analyzedRepository.indicators ){
                sb.append(analyzedRepository.fullName).append(";")
                sb.append(branch.name).append(";")

                for(indicatorToOutput in indicatorsToOutput){
                    sb.append(actualIndicators.get(indicatorToOutput) ?: "N/A").append(";")
                }

                sb.append(System.lineSeparator());
            }

            writer.write(sb.toString())

        } catch (e: IOException) {
            log.error("problem while writing to output file", e)
        }

        writer.close()

    }
}
