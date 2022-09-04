package io.liquirium.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacCalculator {

  def sha256(data: String, key: String): String = getHmac("HmacSHA256", data, key)

  def sha512(data: String, key: String): String = getHmac("HmacSHA512", data, key)

  def sha384(data: String, key: String): String = getHmac("HmacSHA384", data, key)


  private def getHmac(algo: String, data: String, key: String): String = {
    val signingKey = new SecretKeySpec(key.getBytes("UTF-8"), algo)

    val mac = Mac.getInstance(algo)
    mac.init(signingKey)

    val rawHmac = mac.doFinal(data.getBytes("UTF-8"))
    byteArrayToHex(rawHmac)
  }

  private def byteArrayToHex(a: Array[Byte]): String = {
    val sb = new StringBuilder(a.length * 2)
    a.foreach { b => sb.append("%02x".format(b & 0xff)) }
    sb.toString()
  }

}
