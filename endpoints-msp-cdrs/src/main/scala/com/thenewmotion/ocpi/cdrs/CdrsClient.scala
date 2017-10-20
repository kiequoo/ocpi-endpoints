package com.thenewmotion.ocpi
package cdrs

import java.time.ZonedDateTime

import akka.NotUsed
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import common.{ErrUnMar, OcpiClient, PaginatedSource, SucUnMar}
import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.concurrent.{ExecutionContext, Future}
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.Cdr
import msgs.AuthToken

class CdrsClient(implicit http: HttpExt) extends OcpiClient {

  def getCdrs(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None
  )(
    implicit ec: ExecutionContext,
    mat: Materializer,
    successU: SucUnMar[Cdr],
    errorU: ErrUnMar
  ): Future[ErrorRespOr[Iterable[Cdr]]] =
    traversePaginatedResource[Cdr](uri, auth, dateFrom, dateTo)

  def cdrsSource(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None
  )(
    implicit ec: ExecutionContext,
    mat: Materializer,
    successU: SucUnMar[Cdr],
    errorU: ErrUnMar
  ): Source[Cdr, NotUsed] =
    PaginatedSource[Cdr](http, uri, auth, dateFrom, dateTo)

}
