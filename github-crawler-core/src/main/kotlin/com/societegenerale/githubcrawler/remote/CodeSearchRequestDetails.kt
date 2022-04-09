package com.societegenerale.githubcrawler.remote


//@JsonSerialize(using = CodeSearchRequestDetailsSerializer::class)
class CodeSearchRequestDetails(val searchText : String, val filters : CodeSearchFilter)