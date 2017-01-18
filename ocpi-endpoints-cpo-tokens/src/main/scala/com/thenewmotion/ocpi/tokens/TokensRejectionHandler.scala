package com.thenewmotion.ocpi
package tokens

import akka.http.scaladsl.server.Rejection

case class TokenErrorRejection(error: TokenError) extends Rejection

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server.directives.MiscDirectives

object TokensRejectionHandler extends BasicDirectives
  with MiscDirectives with SprayJsonSupport {

  import TokenError._
  import akka.http.scaladsl.model.StatusCodes._
  import akka.http.scaladsl.server.AuthorizationFailedRejection
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server.RejectionHandler
  import msgs.v2_1.CommonTypes.ErrorResp
  import msgs.v2_1.OcpiJsonProtocol._
  import msgs.v2_1.OcpiStatusCode.GenericClientFailure

  val Default = RejectionHandler.newBuilder().handle {
    case AuthorizationFailedRejection => extractUri { uri =>
      complete {
        Forbidden -> ErrorResp(
          GenericClientFailure,
          Some(s"The client is not authorized to access ${uri.toRelative}")
        )
      }
    }
    case TokenErrorRejection(TokenNotFound(reason)) => complete {
      NotFound -> ErrorResp(GenericClientFailure, reason)
    }
    case TokenErrorRejection(TokenCreationFailed(reason)) => complete {
      OK -> ErrorResp(GenericClientFailure, reason)

    }
    case TokenErrorRejection(TokenUpdateFailed(reason)) => complete {
      OK -> ErrorResp(GenericClientFailure, reason)
    }
  }.result()
}
