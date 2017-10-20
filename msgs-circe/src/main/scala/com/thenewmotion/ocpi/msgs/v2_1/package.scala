package com.thenewmotion.ocpi.msgs

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.Configuration
import cats.syntax.either._
import scala.util.Try

package object v2_1 {
  implicit val config: Configuration = Configuration.default.withSnakeCaseKeys.withDefaults

  def stringEncoder[T](enc: T => String): Encoder[T] =
    Encoder.encodeString.contramap[T](enc)

  def tryStringDecoder[T](dec: String => T): Decoder[T] =
    Decoder.decodeString.emap { x =>
      Try(dec(x)).toEither.leftMap(_.getMessage)
    }
}
