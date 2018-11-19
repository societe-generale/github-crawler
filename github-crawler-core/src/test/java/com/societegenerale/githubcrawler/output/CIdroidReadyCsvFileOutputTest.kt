package com.societegenerale.githubcrawler.output

import com.societegenerale.githubcrawler.model.Branch
import com.societegenerale.githubcrawler.model.Repository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.util.*

class CIdroidReadyCsvFileOutputTest {


    @Test
    fun shouldGenerateExpectedFile() {

        val cIdroidReadyCsvFileOutput = CIdroidReadyCsvFileOutput(listOf("indic1", "indic2"))

        val repo1masterBranchIndicators = mapOf(Pair("indic1", "value1_forRepo1"), Pair("indic2", "value2_forRepo1"))
        val repo1Indicators = mapOf(Pair(Branch("master"), repo1masterBranchIndicators))
        val repo1 = buildRepoWithIndicators("orga/repo1", repo1Indicators)

        val repo2 = buildRepoWithIndicators("orga/repo2", emptyMap())

        val repo3someBranchIndicators = mapOf(Pair("indic1", "value1_forRepo3"), Pair("indic2", "value2_forRepo3"), Pair("indic3", "value3_forRepo3"))
        val repo3Indicators = mapOf(Pair(Branch("someBranch"), repo3someBranchIndicators))
        val repo3 = buildRepoWithIndicators("orga/repo3", repo3Indicators)


        cIdroidReadyCsvFileOutput.output(repo1);
        cIdroidReadyCsvFileOutput.output(repo2);
        cIdroidReadyCsvFileOutput.output(repo3);

        val expectedFileContent = "repositoryFullName;branchName;indic1;indic2" + System.lineSeparator() +
                "orga/repo1;master;value1_forRepo1;value2_forRepo1;" + System.lineSeparator() +
                "orga/repo3;someBranch;value1_forRepo3;value2_forRepo3;" + System.lineSeparator()

        val generatedFile = File(".").walkBottomUp()
                .filter { f -> f.isFile }
                .findLast { f -> f.name.startsWith(cIdroidReadyCsvFileOutput.getPrefix()) }

        assertThat(generatedFile?.readText(Charsets.UTF_8)).isEqualTo(expectedFileContent)

    }

    private fun buildRepoWithIndicators(fullName: String, indicators: Map<Branch, Map<String, String>>): Repository {

        return Repository(name = "dummyRepo",
                creationDate = Date(),
                config = null,
                defaultBranch = "master",
                fullName = fullName,
                lastUpdateDate = Date(),
                reason = null,
                url = "http://hello",
                indicators = indicators,
                branchesToParse = listOf(Branch("master"))
        )

    }

}