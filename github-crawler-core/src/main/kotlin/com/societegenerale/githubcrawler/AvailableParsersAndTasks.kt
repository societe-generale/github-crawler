package com.societegenerale.githubcrawler

import com.societegenerale.githubcrawler.parsers.FileContentParser
import com.societegenerale.githubcrawler.repoTaskToPerform.RepoTaskBuilder
import org.slf4j.LoggerFactory

class AvailableParsersAndTasks(fileContentParsers: List<FileContentParser>,
                               repoTasksBuilders: List<RepoTaskBuilder>) {


  val log = LoggerFactory.getLogger(this.javaClass)

  //TODO validate we don't have 2 parsers with same nameInConfig

  private val availableFileContentParsers = fileContentParsers.map{it.getNameInConfig() to it}.toMap()

  private val availableRepoTasksBuilders = repoTasksBuilders.map{it.type to it}.toMap()

  init{
    log.info("--> ${availableFileContentParsers.size} available parser(s)..")
    log.info("--> ${availableRepoTasksBuilders.size} taskBuilder(s) available : "+availableRepoTasksBuilders.keys.joinToString(separator = ", "))
  }

  fun getParserByName(parserName : String) : FileContentParser{
    val parser= availableFileContentParsers.get(parserName)

    if(parser==null){
      throw IllegalStateException("fileContentParserName is unknown : $parserName. please double check your config, the name must match one of the known values")
    }
    else{
      return parser
    }
  }

  fun getRepoTasksBuilderByType(repoTasksBuilderType : String) : RepoTaskBuilder{
    val repoTasksBuilder= availableRepoTasksBuilders.get(repoTasksBuilderType)

    if(repoTasksBuilder==null){
      throw IllegalStateException("repoTasksBuilderType is unknown : $repoTasksBuilderType. please double check your config, the type must match one of the known values")
    }
    else{
      return repoTasksBuilder
    }
  }




}