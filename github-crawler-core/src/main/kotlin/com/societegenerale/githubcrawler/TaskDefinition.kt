package com.societegenerale.githubcrawler

//TODO check regularly on https://github.com/spring-projects/spring-boot/issues/8762 if it's solved.
class TaskDefinition(var name: String = "N/A",
                     var type: String= "N/A",
                     var params: Map<String,String> = HashMap())