package com.societegenerale.githubcrawler.remote

import com.fasterxml.jackson.core.JsonGenerator

import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class CodeSearchRequestDetailsSerializer() : StdSerializer<CodeSearchRequestDetails>(CodeSearchRequestDetails::class.java) {

  override fun serialize(p0: CodeSearchRequestDetails?, p1: JsonGenerator?, p2: SerializerProvider?) {
    TODO("Not yet implemented")
  }

}
