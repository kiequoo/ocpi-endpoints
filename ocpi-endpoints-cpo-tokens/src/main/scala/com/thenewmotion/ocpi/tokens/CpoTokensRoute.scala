package com.thenewmotion.ocpi
package tokens

class CpoTokensRoute(
  service: CpoTokensService
) extends JsonApi {

  import akka.http.scaladsl.model.StatusCodes
  import akka.http.scaladsl.server.PathMatcher1
  import akka.http.scaladsl.server.Route
  import com.thenewmotion.mobilityid.CountryCode
  import com.thenewmotion.mobilityid.OperatorIdIso
  import msgs.v2_1.CommonTypes.SuccessResp
  import msgs.v2_1.CommonTypes.SuccessWithDataResp
  import msgs.v2_1.OcpiStatusCode.GenericSuccess
  import msgs.v2_1.Tokens._

  import scala.concurrent.Future
  import scalaz._

  private def leftToRejection[T](errOrX: Future[TokenError \/ T])(f: T => Route): Route =
    onSuccess(errOrX) {
      case -\/(e) => reject(TokenErrorRejection(e))
      case \/-(r) => f(r)
    }

  private val CountryCodeSegment: PathMatcher1[CountryCode] = Segment.map(CountryCode(_))
  private val OperatorIdSegment: PathMatcher1[OperatorIdIso] = Segment.map(OperatorIdIso(_))
  private def isResourceAccessAuthorized(apiUser: ApiUser, cc: CountryCode, opId: OperatorIdIso) =
    authorize(CountryCode(apiUser.countryCode) == cc && OperatorIdIso(apiUser.partyId) == opId)

  import msgs.v2_1.OcpiJsonProtocol._

  def route(apiUser: ApiUser) = handleRejections(TokensRejectionHandler.Default) {
    pathPrefix(CountryCodeSegment / OperatorIdSegment / Segment) { (cc, opId, tokenUid) =>
      pathEndOrSingleSlash {
        put {
          isResourceAccessAuthorized(apiUser, cc, opId) {
            entity(as[Token]) { token =>
              leftToRejection(service.createOrUpdateToken(cc, opId, tokenUid, token)) { created =>
                complete {
                  val successResp = SuccessResp(GenericSuccess)
                  if (created) (StatusCodes.Created, successResp)
                  else (StatusCodes.OK, successResp)
                }
              }
            }
          }
        } ~
        patch {
          isResourceAccessAuthorized(apiUser, cc, opId) {
            entity(as[TokenPatch]) { patch =>
              leftToRejection(service.updateToken(cc, opId, tokenUid, patch)) { _ =>
                complete(SuccessResp(GenericSuccess))
              }
            }
          }
        } ~
        get {
          isResourceAccessAuthorized(apiUser, cc, opId) {
            leftToRejection(service.token(cc, opId, tokenUid)) { token =>
              complete(SuccessWithDataResp(GenericSuccess, None, data = token))
            }
          }
        }
      }
    }
  }
}
