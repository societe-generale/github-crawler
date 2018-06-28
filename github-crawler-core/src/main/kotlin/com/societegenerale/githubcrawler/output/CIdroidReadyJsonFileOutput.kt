package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class CIdroidReadyJsonFileOutput @Throws(IOException::class)

constructor(val indicatorsToOutput: List<String>) : GitHubCrawlerOutput {

    val log = LoggerFactory.getLogger(this.javaClass)

    private val finalOutputFileName: String

    init {

        val now = LocalDateTime.now()

        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        finalOutputFileName = "CIdroidReadyContent_" + now.format(formatter) + ".json"

        val writer = openFileWithOptions(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)

        //initiating the Json array
        writer.write("[\n");

        writer.close()
    }

    @Throws(IOException::class)
    override fun output(analyzedRepository: Repository) {

        openFileWithOptions(StandardOpenOption.WRITE, StandardOpenOption.APPEND).use {

            val sb=StringBuilder()

            for((branch,actualIndicators) in analyzedRepository.indicators ){
                sb.append("{")
                sb.append("\"repoFullName\": \"").append(analyzedRepository.fullName).append("\",")
                //we take the first indicator value, assuming it's a path to a file
                sb.append("\"filePathOnRepo\": \"").append(actualIndicators.get(indicatorsToOutput[0])).append("\",")

                appendOtherIndicatorsIfAny(sb, actualIndicators)

                sb.append("\"branchName\": \"").append(branch.name).append("\"")

                sb.append("},")

                sb.append(System.lineSeparator())
            }

            it.write(sb.toString())
        }

    }

    private fun appendOtherIndicatorsIfAny(sb: StringBuilder, actualIndicators: Map<String, String>) {
        if (indicatorsToOutput.size > 1) {
            //if there are others, we output them but they probbaly won't be processed given their name
            for (i in 1..indicatorsToOutput.size - 1) {
                sb.append("\"otherIndicator$i\": \"").append(actualIndicators.get(indicatorsToOutput[i])).append("\",")
            }
        }
    }

    @Throws(IOException::class)
    override fun finalizeOutput(){
        val content = Scanner(File(finalOutputFileName)).useDelimiter("\\Z").next()
        //removing the last character which is a "," from the json
        val withoutLastCharacter = content.substring(0, content.length - 1)

        val writer = openFileWithOptions(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)

        writer.write(withoutLastCharacter)

        //closing the Json array
        writer.write("\n]");

        writer.close()
    }

    @Suppress("SpreadOperator") //no performance impact given the number of values (a couple, max)..
    private fun openFileWithOptions(vararg options : OpenOption) : BufferedWriter {

        return Files.newBufferedWriter(Paths.get(finalOutputFileName),
                StandardCharsets.UTF_8,
                *options)
    }
}
