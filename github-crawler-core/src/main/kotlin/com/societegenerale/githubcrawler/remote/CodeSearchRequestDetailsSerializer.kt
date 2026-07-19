package com.societegenerale.githubcrawler.remote

import tools.jackson.core.JsonGenerator

import tools.jackson.databind.ser.std.StdSerializer
import tools.jackson.databind.SerializationContext

class CodeSearchRequestDetailsSerializer() : StdSerializer<CodeSearchRequestDetails>(CodeSearchRequestDetails::class.java) {

  override fun serialize(p0: CodeSearchRequestDetails?, p1: JsonGenerator?, p2: SerializationContext?) {
    TODO("Not yet implemented")
  }

}
