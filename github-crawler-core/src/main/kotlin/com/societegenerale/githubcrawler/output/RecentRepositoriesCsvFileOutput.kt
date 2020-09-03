package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Repository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*

/**
 * outputting the creation and last update date of all repositories
 */
class RecentRepositoriesCsvFileOutput() : CsvFileOutput(emptyList<String>()) {

  override val log: Logger
    get() = LoggerFactory.getLogger(this.javaClass)


  override fun getCsvHeaderFrom(initParam: Any): String {
    return "repositoryName;creationDate;lastUpdateDate"
  }

  override fun getPrefix(): String {
    return "RecentRepositories_"
  }

  override fun outputRepository(analyzedRepository: Repository): StringBuilder {

    val sb = StringBuilder()

    sb.append(analyzedRepository.name).append(";")
    sb.append(analyzedRepository.creationDate.formatted()).append(";")
    sb.append(analyzedRepository.lastUpdateDate.formatted()).append(";")
    sb.append("\n")

    return sb
  }

  private fun Date.formatted(): String{
    val sdf= SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(this)
  }

}
