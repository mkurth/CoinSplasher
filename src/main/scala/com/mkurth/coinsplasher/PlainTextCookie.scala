package com.mkurth.coinsplasher

import cats.{Id, Monad}
import org.scalajs.dom.crypto.{AesCtrParams, AesKeyAlgorithm, CryptoKey, GlobalCrypto, KeyFormat, KeyUsage}
import org.scalajs.dom.document
import org.scalajs.dom.html.Document

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.higherKinds
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array, byteArray2Int8Array}
import scala.util.Try

sealed trait Cookie[F[_]] {
  def read: F[Map[String, String]]

  def update(key: String, value: String): F[Map[String, String]]
}

object PlainTextCookie {

  def parseStringToCookie(string: String): Map[String, String] = {
    string.split(";")
      .map(entry => {
        Try(entry.split("=").toList match {
          case key :: value :: _ => Some(key.trim -> value)
        }).getOrElse(None)
      }).collect({
      case Some(tuple) => tuple
    }).toMap
  }

  def parseCookieToString(content: Map[String, String]): List[String] = {
    content.map({
      case (key, value) => s"$key=$value"
    }).toList
  }

  def apply(document: Document): Cookie[Id] = new Cookie[Id] {
    def read: Map[String, String] = parseStringToCookie(document.cookie)

    def update(key: String, value: String): Map[String, String] = {
      val updatedCookie = read.updated(key, value)
      parseCookieToString(updatedCookie).foreach(document.cookie = _)
      updatedCookie
    }
  }

  def apply: Cookie[Id] = PlainTextCookie(document)
}

object EncryptedCookie {
  private def aesCtrParams(passphrase: String) = AesCtrParams("aes-ctr", byteArray2Int8Array(repeatUntil(passphrase.getBytes, 16)), 64)

  private def repeatUntil(passphrase: Array[Byte], bytes: Int): Array[Byte] = {
    if (passphrase.length < bytes) {
      repeatUntil(passphrase ++ passphrase, bytes)
    } else passphrase.take(bytes)
  }

  private def eventualKey(passphrase: String) = {
    GlobalCrypto
      .crypto
      .subtle
      .importKey(
        format = KeyFormat.raw,
        keyData = byteArray2Int8Array(repeatUntil(passphrase.getBytes, 32)),
        algorithm = AesKeyAlgorithm("aes-ctr", 256),
        extractable = false,
        keyUsages = js.Array(KeyUsage.decrypt, KeyUsage.encrypt)
      ).toFuture
  }

  def encrypt(data: String, passphrase: String): Future[String] = {
    eventualKey(passphrase).flatMap(key => {
      GlobalCrypto.crypto.subtle.encrypt(
        algorithm = aesCtrParams(passphrase),
        key = key.asInstanceOf[CryptoKey],
        data = byteArray2Int8Array(data.getBytes)
      ).toFuture
        .map(buf => {
          val bab = buf.asInstanceOf[ArrayBuffer]
          val x = new Uint8Array(bab)
          x.map(_.toHexString).mkString
        })
    })
  }

  def decrypt(data: String, passphrase: String): Future[String] = {
    eventualKey(passphrase).flatMap(key => {
      GlobalCrypto.crypto.subtle.decrypt(
        algorithm = aesCtrParams(passphrase),
        key = key.asInstanceOf[CryptoKey],
        data = byteArray2Int8Array(data.getBytes)
      ).toFuture
        .map(buf => {
          val bab = buf.asInstanceOf[ArrayBuffer]
          val x = new Uint8Array(bab)
          val string = x.map(_.toHexString).mkString
          println(string)
          string
        })
    })
  }

  def apply[F[_] : Monad](document: Document, encrypt: String => F[String], decrypt: String => F[String]): Cookie[F] = new Cookie[F] {
    override def read: F[Map[String, String]] = {
      val head = PlainTextCookie.parseStringToCookie(document.cookie).getOrElse("enc", "")
      Monad[F].map(decrypt(head))(PlainTextCookie.parseStringToCookie)
    }

    override def update(key: String, value: String): F[Map[String, String]] = {
      val updatedCookie: F[Map[String, String]] = Monad[F].map(read)(_.updated(key, value))
      val encrypted = Monad[F].flatMap(updatedCookie)(u => encrypt(PlainTextCookie.parseCookieToString(u).mkString(";")))
      Monad[F].map(encrypted)(data => document.cookie = data)
      updatedCookie
    }
  }

  def apply(document: Document, passphrase: String): Cookie[Future] = {
    new Cookie[Future] {
      override def read: Future[Map[String, String]] = {
        decrypt(document.cookie, passphrase)
          .map(PlainTextCookie.parseStringToCookie)
      }

      override def update(key: String, value: String): Future[Map[String, String]] = {
        read.recover({case e =>
          println(e)
          Map[String, String]()
        }).map(_.updated(key, value))
          .map(PlainTextCookie.parseCookieToString)
          .map(_.mkString(";"))
          .flatMap(cookieString => encrypt(cookieString, passphrase))
          .flatMap(encryptedCookie => {
            document.cookie = "enc=" + encryptedCookie
            read
          })
      }
    }
  }

  def apply(passphrase: String): Cookie[Future] = EncryptedCookie(document, passphrase)
}
