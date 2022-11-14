package com.societegenerale.githubcrawler

import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.lang.Nullable


data class FileToParse(val name: String,
                       val redirectTo: String?)

class FileToParseConversionService : ConversionService {

    override fun canConvert(@Nullable aClass: Class<*>?, aClass1: Class<*>): Boolean {
        return false
    }

    override fun canConvert(@Nullable sourcetype: TypeDescriptor?, targetType: TypeDescriptor): Boolean {
        return targetType.name == FileToParse::class.java.name
    }

    override fun <T : Any?> convert(@Nullable o: Any?, targetType: Class<T>): T? {
        return null
    }

    override fun convert(@Nullable value: Any?, @Nullable sourceType: TypeDescriptor?, targetType: TypeDescriptor): Any? {

        return if (targetType.name == FileToParse::class.java.name) {
            FileToParse(value as String, null)
        } else value

    }
}

