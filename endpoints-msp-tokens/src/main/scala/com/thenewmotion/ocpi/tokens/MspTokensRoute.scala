package com.thenewmotion.ocpi
package tokens

import java.time.ZonedDateTime

import _root_.akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import _root_.akka.http.scaladsl.model.StatusCode
import _root_.akka.http.scaladsl.model.StatusCodes.{NotFound, OK}
import _root_.akka.http.scaladsl.server.{Directive1, Route}
import _root_.akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromRequestUnmarshaller}
import common._
import msgs.{ErrorResp, GlobalPartyId, SuccessResp}
import msgs.OcpiStatusCode._
import msgs.v2_1.Tokens.{AuthorizationInfo, LocationReferences, Token, TokenUid}
import tokens.AuthorizeError._

import scala.concurrent.ExecutionContext

object MspTokensRoute {
  def apply(
    service: MspTokensService,
    DefaultLimit: Int = 1000,
    MaxLimit: Int = 1000
  )(
    implicit pagTokensM: SuccessRespMar[Iterable[Token]],
    authM: SuccessRespMar[AuthorizationInfo],
    errorM: ErrRespMar,
    locationReferencesU: FromEntityUnmarshaller[LocationReferences]
  ) = new MspTokensRoute(service, DefaultLimit, MaxLimit)
}

class MspTokensRoute private[ocpi](
  service: MspTokensService,
  val DefaultLimit: Int,
  val MaxLimit: Int
)(
  implicit pagTokensM: SuccessRespMar[Iterable[Token]],
  authM: SuccessRespMar[AuthorizationInfo],
  errorM: ErrRespMar,
  locationReferencesU: FromEntityUnmarshaller[LocationReferences]
) extends OcpiDirectives
    with PaginatedRoute
    with EitherUnmarshalling {

  implicit def locationsErrorResp(
    implicit errorMarshaller: ToResponseMarshaller[(StatusCode, ErrorResp)],
    statusMarshaller: ToResponseMarshaller[StatusCode]
  ): ToResponseMarshaller[AuthorizeError] =
    Marshaller { implicit ex: ExecutionContext =>
      {
        case _: MustProvideLocationReferences.type => errorMarshaller(OK -> ErrorResp(NotEnoughInformation))
        case _: TokenNotFound.type                 => statusMarshaller(NotFound)
      }
    }

  // akka-http doesn't handle optional entity, see https://github.com/akka/akka-http/issues/284
  def optionalEntity[T](unmarshaller: FromRequestUnmarshaller[T]): Directive1[Option[T]] =
    entity(as[String]).flatMap { stringEntity =>
      if (stringEntity == null || stringEntity.isEmpty) {
        provide(Option.empty[T])
      } else {
        entity(unmarshaller).flatMap(e => provide(Some(e)))
      }
    }

  private val TokenUidSegment = Segment.map(TokenUid(_))

  def apply(
    apiUser: GlobalPartyId
  )(
    implicit ec: ExecutionContext
  ): Route =
    pathEndOrSingleSlash {
      get {
        paged { (pager: Pager, dateFrom: Option[ZonedDateTime], dateTo: Option[ZonedDateTime]) =>
          onSuccess(service.tokens(apiUser, pager, dateFrom, dateTo)) { pagTokens =>
            respondWithPaginationHeaders(pager, pagTokens) {
              complete(SuccessResp(GenericSuccess, data = pagTokens.result))
            }
          }
        }
      }
    } ~
    pathPrefix(TokenUidSegment) { tokenUid =>
      path("authorize") {
        (post & optionalEntity(as[LocationReferences])) { lr =>
          complete {
            service.authorize(apiUser, tokenUid, lr).mapRight { authInfo =>
              SuccessResp(GenericSuccess, data = authInfo)
            }
          }
        }
      }
    }
}
