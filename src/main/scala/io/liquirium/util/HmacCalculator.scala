package io.liquirium.util

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.collection.mutable

object HmacCalculator {

  def sha256(data: String, key: String): String = getHmac("HmacSHA256", data, key)

  def sha256Base64(data: String, key: String): String = getHmacBase64("HmacSHA256", data, key)

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
    val sb = new mutable.StringBuilder(a.length * 2)
    a.foreach { b => sb.append("%02x".format(b & 0xff)) }
    sb.toString()
  }

  //So nix, aber wusste nich wie man getHmac und byteArrayToHex am besten trennt, deshalb erstmal so doppel gemoppelt
  private def getHmacBase64(algo: String, data: String, key: String): String = {
    val signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algo)

    val mac = Mac.getInstance(algo)
    mac.init(signingKey)

    val hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8))
    Base64.getEncoder.encodeToString(hash)
  }

}
