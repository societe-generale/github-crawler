package com.societegenerale.githubcrawler


data class RepositoryConfig(val excluded: Boolean = false,
                            val filesToParse: List<com.societegenerale.githubcrawler.FileToParse> = ArrayList(),
                            val tags: List<String> = ArrayList()
)
