package com.societegenerale.githubcrawler

import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor


data class FileToParse(val name: String,
                       val redirectTo: String?)

class FileToParseConversionService : ConversionService {

    override fun canConvert(aClass: Class<*>, aClass1: Class<*>): Boolean {
        return false
    }

    override fun canConvert(sourcetype: TypeDescriptor, targetType: TypeDescriptor): Boolean {
        return targetType.name == FileToParse::class.java.name
    }

    override fun <T> convert(o: Any, aClass: Class<T>): T? {
        return null
    }

    override fun convert(value: Any, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any {

        return if (targetType.name == FileToParse::class.java.name) {
            FileToParse(value as String, null)
        } else value

    }
}

