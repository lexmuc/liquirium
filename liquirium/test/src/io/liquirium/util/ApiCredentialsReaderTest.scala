package io.liquirium.util

import io.liquirium.core.helpers.TestWithMocks
import io.liquirium.util.helpers.FakeFileIO

import java.nio.file.Path

class ApiCredentialsReaderTest extends TestWithMocks {

  test("it reads the api credentials from the given files with api key and secret") {
    val path = mock[Path]
    val fileContents =
      """
        |{
        |  "apiKey": "my-api-key",
        |  "secret": "my-secret"
        |}
        |""".stripMargin
    val fileIO = new FakeFileIO(Map(path -> fileContents))
    val apiCredentialsReader = new ApiCredentialsReader(fileIO)
    apiCredentialsReader.read(path) shouldEqual ApiCredentials("my-api-key", "my-secret")
  }

}
