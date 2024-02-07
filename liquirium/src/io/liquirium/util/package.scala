package io.liquirium

package object util {

  def apiCredentialsReader: ApiCredentialsReader = new ApiCredentialsReader(ProductionFileIO)

}
