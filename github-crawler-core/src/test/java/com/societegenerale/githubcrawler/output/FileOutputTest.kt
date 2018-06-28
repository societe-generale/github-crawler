package com.societegenerale.githubcrawler.output


import com.societegenerale.githubcrawler.model.Repository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


class FileOutputTest{

    var resolver = PathMatchingResourcePatternResolver(this.javaClass.classLoader);

    @Before
    fun cleanUp(){

        val resources = resolver.getResources("file:somePrfix*.txt")

        for(existingFile in resources){
            val fileToDeletePath = Paths.get(existingFile.uri)
            Files.delete(fileToDeletePath)
        }


    }

    @Test
    fun shouldOutputFileAsExpected(){

        val fileOutput = FileOutput("somePrfix")

        val repoToOutput1 = Repository(name = "repo1",
                                        creationDate = Date(),
                                        config= null,
                                        defaultBranch = "master",
                                        fullName = "orgName/repoName1",
                                        lastUpdateDate = Date(),
                                        ownerTeam = null,
                                        reason = null,
                                        url="http://hello"
                                        )

        val repoToOutput2 = Repository(name = "repo2",
                creationDate = Date(),
                config= null,
                defaultBranch = "master",
                fullName = "orgName/repoName2",
                lastUpdateDate = Date(),
                ownerTeam = null,
                reason = null,
                url="http://hello2"
        )

        fileOutput.output(repoToOutput1)
        fileOutput.output(repoToOutput2)

        val resources = resolver.getResources("file:somePrfix*.txt") // yields empty array

        assertThat(resources).hasSize(1)

        val lines=Files.readAllLines(Paths.get(resources[0].uri))

        assertThat(lines).hasSize(3)

        //not a great test.. would need to inject a fake DateTi;eProvider to control precisely output
        assertThat(lines[0]).startsWith("OUTPUT FOR GitHub crawler - ");

        assertThat(lines[1]).startsWith("Repository(url=http://hello, name=repo1, defaultBranch=master, creationDate=")
        assertThat(lines[1]).contains(", excluded=false, config=null, reason=null, skipped=false, fullName=orgName/repoName1, indicators={}, branchesToParse=[], tags=[], groups=[], crawlerRunId=NO_CRAWLER_RUN_ID_DEFINED, searchResults={}, ownerTeam=null)")

        assertThat(lines[2]).startsWith("Repository(url=http://hello2, name=repo2, defaultBranch=master, creationDate=")
        assertThat(lines[2]).endsWith(", excluded=false, config=null, reason=null, skipped=false, fullName=orgName/repoName2, indicators={}, branchesToParse=[], tags=[], groups=[], crawlerRunId=NO_CRAWLER_RUN_ID_DEFINED, searchResults={}, ownerTeam=null)")





    }



}

