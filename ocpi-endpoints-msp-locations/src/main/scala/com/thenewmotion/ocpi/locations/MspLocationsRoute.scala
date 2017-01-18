package com.thenewmotion.ocpi
package locations

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.PathMatcher1
import akka.http.scaladsl.server.Route
import com.thenewmotion.mobilityid.CountryCode
import com.thenewmotion.mobilityid.OperatorIdIso
import msgs.v2_1.CommonTypes.SuccessResp
import msgs.v2_1.CommonTypes.SuccessWithDataResp
import msgs.v2_1.Locations._
import msgs.v2_1.OcpiStatusCode._

import scala.concurrent.Future
import scalaz._

class MspLocationsRoute(
  service: MspLocationsService
) extends JsonApi {

  import msgs.v2_1.OcpiJsonProtocol._

  private def leftToRejection[T](errOrX: Future[LocationsError \/ T])(f: T => Route): Route =
    onSuccess(errOrX) {
      case -\/(e) => reject(LocationsErrorRejection(e))
      case \/-(r) => f(r)
    }

  def route(apiUser: ApiUser) =
    handleRejections(LocationsRejectionHandler.Default)(routeWithoutRh(apiUser))

  private val CountryCodeSegment: PathMatcher1[CountryCode] = Segment.map(CountryCode(_))
  private val OperatorIdSegment: PathMatcher1[OperatorIdIso] = Segment.map(OperatorIdIso(_))
  private def isResourceAccessAuthorized(apiUser: ApiUser, cc: CountryCode, opId: OperatorIdIso) =
    authorize(CountryCode(apiUser.countryCode) == cc && OperatorIdIso(apiUser.partyId) == opId)

  private[locations] def routeWithoutRh(apiUser: ApiUser) = {
    pathPrefix(CountryCodeSegment / OperatorIdSegment / Segment) { (cc, opId, locId) =>
      pathEndOrSingleSlash {
        put {
          // TODO: why is the operator Id not checked here?
          authorize(CountryCode(apiUser.countryCode) == cc) {
            entity(as[Location]) { location =>
              leftToRejection(service.createOrUpdateLocation(cc, opId, locId, location)) { res =>
                complete((if (res) StatusCodes.Created else StatusCodes.OK, SuccessResp(GenericSuccess)))
              }
            }
          }
        } ~
        patch {
          isResourceAccessAuthorized(apiUser, cc, opId) {
            entity(as[LocationPatch]) { location =>
              leftToRejection(service.updateLocation(cc, opId, locId, location)) { _ =>
                complete(SuccessResp(GenericSuccess))
              }
            }
          }
        } ~
        get {
          isResourceAccessAuthorized(apiUser, cc, opId) {
            leftToRejection(service.location(cc, opId, locId)) { location =>
              complete(SuccessWithDataResp(GenericSuccess, None, data = location))
            }
          }
        }
      } ~
      isResourceAccessAuthorized(apiUser, cc, opId) {
        pathPrefix(Segment) { evseId =>
          pathEndOrSingleSlash {
            put {
              entity(as[Evse]) { evse =>
                leftToRejection(service.addOrUpdateEvse(cc, opId, locId, evseId, evse)) { res =>
                  complete((if (res) StatusCodes.Created else StatusCodes.OK, SuccessResp(GenericSuccess)))
                }
              }
            } ~
            patch {
              entity(as[EvsePatch]) { evse =>
                leftToRejection(service.updateEvse(cc, opId, locId, evseId, evse)) { _ =>
                  complete(SuccessResp(GenericSuccess))
                }
              }
            } ~
            get {
              leftToRejection(service.evse(cc, opId, locId, evseId)) { evse =>
                complete(SuccessWithDataResp(GenericSuccess, None, data = evse))
              }
            }
          } ~
          (path(Segment) & pathEndOrSingleSlash) { connId =>
            put {
              entity(as[Connector]) { conn =>
                leftToRejection(service.addOrUpdateConnector(cc, opId, locId, evseId, connId, conn)) { res =>
                  complete((if (res) StatusCodes.Created else StatusCodes.OK, SuccessResp(GenericSuccess)))
                }
              }
            } ~
            patch {
              entity(as[ConnectorPatch]) { conn =>
                leftToRejection(service.updateConnector(cc, opId, locId, evseId, connId, conn)) { _ =>
                  complete(SuccessResp(GenericSuccess))
                }
              }
            } ~
            get {
              leftToRejection(service.connector(cc, opId, locId, evseId, connId)) { connector =>
                complete(SuccessWithDataResp(GenericSuccess, None, data = connector))
              }
            }
          }
        }
      }
    }
  }
}
