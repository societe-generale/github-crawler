package com.societegenerale.githubcrawler

//TODO check regularly on https://github.com/spring-projects/spring-boot/issues/8762 if it's solved.
// config properties can't binded as immutable values, so we need to have a workaround, declaring all attributes as mutable
// in previous version, name and type were lateinit, but in that case, it wasn't possible (or I didn't find) a way to set the values in unit tests.
// we should roll this back to immutable variables whenever Spring Boot will allow us to do it
class IndicatorDefinition(var name: String="N/A",
                          var type: String = "N/A",
                          var params: Map<String,String> = HashMap())