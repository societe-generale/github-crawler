package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*


class SearchPatternInCodeCsvFileOutputTest{

    private val SEARCH_NAME_TO_OUTPUT="searchNameToOutput"

    val searchPatternInCodeCsvFileOutput = SearchPatternInCodeCsvFileOutput(SEARCH_NAME_TO_OUTPUT)

    @Test
    fun shouldGenerateExpectedFile(){

        val repo1= buildRepoWithSearchResult("orga/repo1",listOf("repo1/path1","repo1/subDir/path2"))

        val repo2= buildRepoWithSearchResult("orga/repo2",emptyList())

        val repo3= buildRepoWithSearchResult("orga/repo3",listOf("repo3/path3"))


        searchPatternInCodeCsvFileOutput.output(repo1);
        searchPatternInCodeCsvFileOutput.output(repo2);
        searchPatternInCodeCsvFileOutput.output(repo3);

        val expectedFileContent="repositoryFullName;location"+System.lineSeparator()+
                                "orga/repo1;repo1/path1;"+System.lineSeparator()+
                                "orga/repo1;repo1/subDir/path2;"+System.lineSeparator()+
                                "orga/repo3;repo3/path3;"+System.lineSeparator()


        val generatedFile= File(".").walkBottomUp()
                                    .filter{f -> f.isFile}
                                    .findLast{f -> f.name.startsWith(searchPatternInCodeCsvFileOutput.getPrefix())}

        assertThat(generatedFile?.readText(Charsets.UTF_8)).isEqualTo(expectedFileContent)

    }

    private fun buildRepoWithSearchResult(fullName : String, searchResults : List<String>) : Repository{

        return Repository(name = "dummyRepo",
                creationDate = Date(),
                config = null,
                defaultBranch = "master",
                fullName = fullName,
                lastUpdateDate = Date(),
                reason = null,
                url = "http://hello",
                miscTasksResults= mapOf(Pair(Branch("master"),mapOf(Pair(SEARCH_NAME_TO_OUTPUT,searchResults)))),
                branchesToParse = setOf(Branch("master"))
                )

    }

}