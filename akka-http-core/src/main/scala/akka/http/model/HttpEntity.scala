/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.http.model

import language.implicitConversions
import java.io.File
import org.reactivestreams.api.Producer
import akka.util.ByteString
import akka.stream2.StreamProducer

/**
 * Models the entity (aka "body" or "content) of an HTTP message.
 */
sealed trait HttpEntity {
  /**
   * Determines whether this entity is known to be empty.
   */
  def isEmpty: Boolean

  /**
   * The `ContentType` associated with this entity.
   */
  def contentType: ContentType
}

object HttpEntity {
  implicit def apply(string: String): Regular = apply(ContentTypes.`text/plain(UTF-8)`, string)
  implicit def apply(bytes: Array[Byte]): Regular = apply(ByteString(bytes))
  implicit def apply(data: ByteString): Regular = apply(ContentTypes.`application/octet-stream`, data)
  def apply(contentType: ContentType, string: String): Regular =
    if (string.isEmpty) Empty else apply(contentType, ByteString(string.getBytes(contentType.charset.nioCharset)))
  def apply(contentType: ContentType, bytes: Array[Byte]): Regular = apply(contentType, ByteString(bytes))
  def apply(contentType: ContentType, data: ByteString): Regular =
    Default(contentType, data.length, StreamProducer.empty)

  def apply(contentType: ContentType, file: File): Regular = {
    val fileLength = file.length
    if (fileLength > 0) Default(contentType, fileLength, StreamProducer.empty) // TODO: attach from-file-Producer
    else empty(contentType)
  }

  val Empty = empty(ContentTypes.`application/octet-stream`)

  def empty(contentType: ContentType): Default =
    Default(contentType = contentType, contentLength = 0, data = StreamProducer.empty)

  /**
   * An HttpEntity that is "well-behaved" according to the HTTP/1.1 spec as that
   * it is either chunked or defines a content-length that is known a-priori.
   * Close-delimited entities are not `Regular` as they exists primarily for backwards compatibility with HTTP/1.0.
   */
  sealed trait Regular extends HttpEntity

  // TODO: re-establish serializability

  /**
   * The model for the entity of a "regular" unchunked HTTP message with a known length.
   */
  case class Default(contentType: ContentType,
                     contentLength: Long,
                     data: Producer[ByteString]) extends Regular {
    require(contentLength >= 0, "contentLength must be non-negative")
    def isEmpty = contentLength == 0
  }

  /**
   * The model for the entity of an HTTP response that is terminated by the server closing the connection.
   * The content-length of such responses is unknown at the time the response headers have been received.
   * Note that this type of HttpEntity cannot be used for HttpRequests!
   */
  case class CloseDelimited(contentType: ContentType, data: Producer[ByteString]) extends HttpEntity {
    def isEmpty = false
  }

  /**
   * The model for the entity of a chunked HTTP message (with `Transfer-Encoding: chunked`).
   */
  case class Chunked(contentType: ContentType, chunks: Producer[ChunkStreamPart]) extends Regular {
    def isEmpty = false
  }

  /**
   * An element of the HttpEntity data stream.
   * Can be either a `Chunk` or a `LastChunk`.
   */
  sealed trait ChunkStreamPart {
    def data: ByteString
  }

  /**
   * An intermediate entity chunk guaranteed to carry non-empty data.
   */
  case class Chunk(data: ByteString, extension: String = "") extends ChunkStreamPart

  /**
   * An intermediate entity chunk guaranteed to carry non-empty data.
   */
  case class LastChunk(extension: String = "", trailer: List[HttpHeader] = Nil) extends ChunkStreamPart {
    def data = ByteString.empty
  }
  object LastChunk extends LastChunk("", Nil)
}