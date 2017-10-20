package com.thenewmotion.ocpi.msgs.v2_1

import java.time.{LocalDate, LocalTime, ZoneOffset, ZonedDateTime}

import com.thenewmotion.ocpi.msgs.{CurrencyCode, ErrorResp, SuccessResp}
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.{GenericClientFailure, GenericSuccess}
import com.thenewmotion.ocpi.ZonedDateTimeParser
import org.specs2.specification.core.Fragments
import scala.language.higherKinds

trait GenericCommonTypesSpec[J, GenericJsonReader[_], GenericJsonWriter[_]] extends
  GenericJsonSpec[J, GenericJsonReader, GenericJsonWriter] {

  def runTests()(
    implicit zonedDateTimeE: GenericJsonWriter[ZonedDateTime],
    zonedDateTimeD: GenericJsonReader[ZonedDateTime],
    errorRespE: GenericJsonWriter[ErrorResp],
    errorRespD: GenericJsonReader[ErrorResp],
    successRespUnitE: GenericJsonWriter[SuccessResp[Unit]],
    successRespUnitD: GenericJsonReader[SuccessResp[Unit]],
    successRespIntE: GenericJsonWriter[SuccessResp[Int]],
    successRespIntD: GenericJsonReader[SuccessResp[Int]],
    currencyCodeE: GenericJsonWriter[CurrencyCode],
    currencyCodeD: GenericJsonReader[CurrencyCode],
    localTimeE: GenericJsonWriter[LocalTime],
    localTimeD: GenericJsonReader[LocalTime],
    localDateE: GenericJsonWriter[LocalDate],
    localDateD: GenericJsonReader[LocalDate]
  ): Fragments = {

    "DateTimeJsonFormat" should {
      val obj = ZonedDateTime.of(2014, 1, 28, 17, 30, 0, 0, ZoneOffset.UTC)
      val str = jsonStringToJson("2014-01-28T17:30:00Z")

      testPair(obj, str)
    }

    "ErrorResp" should {
      testPair(genericErrorResp1, parse(genericErrorRespJson1))
    }

    "SuccessResp" should {
      "handle response without data" in{
        testPair(successResp1, parse(successRespJson1))
      }

      "handle response with data" in {
        testPair(successResp2, parse(successRespJson2))
      }
    }

    "CurrencyCode" should {
      testPair(CurrencyCode("EUR"), jsonStringToJson("EUR"))
    }

    "Time" should {
      testPair(LocalTime.of(13, 25), jsonStringToJson("13:25"))
    }

    "Date" should {
      testPair(LocalDate.of(2017,9, 13), jsonStringToJson("2017-09-13"))
    }
  }

  val date1: ZonedDateTime = ZonedDateTimeParser.parse("2010-01-01T00:00:00Z")

  val successRespJson1: String =
    s"""
       |{
       |  "status_code" : 1000,
       |  "status_message" : "It worked!",
       |  "timestamp": "2010-01-01T00:00:00Z"
       |}
 """.stripMargin

  val successResp1 =
    SuccessResp(
      GenericSuccess,
      Some("It worked!"),
      date1)

  val successRespJson2: String =
    s"""
       |{
       |  "status_code" : 1000,
       |  "status_message" : "It worked!",
       |  "timestamp": "2010-01-01T00:00:00Z",
       |  "data": 42
       |}
 """.stripMargin

  val successResp2 =
    SuccessResp(
      GenericSuccess,
      Some("It worked!"),
      date1, 42)

  val genericErrorRespJson1: String =
    s"""
       |{
       |  "status_code" : 2000,
       |  "status_message" : "Client error",
       |  "timestamp": "2010-01-01T00:00:00Z"
       |}
 """.stripMargin

  val genericErrorResp1 =
    ErrorResp(
      GenericClientFailure,
      Some("Client error"),
      date1)
}
