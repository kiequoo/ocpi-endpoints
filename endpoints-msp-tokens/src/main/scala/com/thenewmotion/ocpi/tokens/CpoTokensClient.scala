package com.thenewmotion.ocpi
package tokens

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.FromByteStringUnmarshaller
import akka.stream.Materializer
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import common.{ClientObjectUri, ErrUnMar, OcpiClient}
import msgs.{AuthToken, SuccessResp}
import msgs.v2_1.Tokens.{Token, TokenPatch, TokenUid}
import cats.syntax.either._

import scala.concurrent.{ExecutionContext, Future}

class CpoTokensClient(implicit http: HttpExt) extends OcpiClient {

  def getToken(
    tokenUri: ClientObjectUri,
    authToken: AuthToken[Ours],
    tokenUid: TokenUid
  )(implicit ec: ExecutionContext,
    mat: Materializer,
    successU: FromByteStringUnmarshaller[SuccessResp[Token]],
    errorU: ErrUnMar
  ): Future[ErrorRespOr[Token]] =
    singleRequest[Token](Get(tokenUri.value), authToken).map {
      _.bimap(err => {
        logger.error(s"Could not retrieve token from ${tokenUri.value}. Reason: $err")
        err
      }, _.data)
    }

  private def push(
    tokenUri: ClientObjectUri,
    authToken: AuthToken[Ours],
    rb: Uri => HttpRequest
  )(implicit ec: ExecutionContext,
    mat: Materializer,
    successU: FromByteStringUnmarshaller[SuccessResp[Unit]],
    errorU: ErrUnMar
  ): Future[ErrorRespOr[Unit]] =
    singleRequest[Unit](rb(tokenUri.value), authToken).map {
      _.bimap(err => {
        logger.error(s"Could not upload token to ${tokenUri.value}. Reason: $err")
        err
      }, _ => ())
    }

  def uploadToken(
    tokenUri: ClientObjectUri,
    authToken: AuthToken[Ours],
    token: Token
  )(implicit ec: ExecutionContext,
    mat: Materializer,
    successU: FromByteStringUnmarshaller[SuccessResp[Unit]],
    errorU: ErrUnMar,
    tokenM: ToEntityMarshaller[Token]
  ): Future[ErrorRespOr[Unit]] =
    push(tokenUri, authToken, uri => Put(uri, token))

  def updateToken(
    tokenUri: ClientObjectUri,
    authToken: AuthToken[Ours],
    patch: TokenPatch
  )(
    implicit ec: ExecutionContext,
    mat: Materializer,
    successU: FromByteStringUnmarshaller[SuccessResp[Unit]],
    errorU: ErrUnMar,
    tokenM: ToEntityMarshaller[TokenPatch]
  ): Future[ErrorRespOr[Unit]] =
    push(tokenUri, authToken, uri => Patch(uri, patch))

}

