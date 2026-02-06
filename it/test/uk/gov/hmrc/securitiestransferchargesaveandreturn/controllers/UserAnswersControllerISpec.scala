/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.securitiestransferchargesaveandreturn.controllers

import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.{SubmissionId, UserAnswers}
import uk.gov.hmrc.securitiestransferchargesaveandreturn.repositories.UserAnswersRepository

import scala.concurrent.{ExecutionContext, Future}


class UserAnswersControllerISpec
  extends AnyWordSpec
    with Matchers
    with OptionValues {
  
  private val saveUrl: String = "/securities-transfer-charge-save-and-return/user-answers"

  private def retrieveUrl(userId: String, submissionId: SubmissionId): String =
    s"/securities-transfer-charge-save-and-return/user-answers/$userId/$submissionId"

  private def retrieveSubmissionIdsUrl(userId: String): String =
    s"/securities-transfer-charge-save-and-return/user-answers/$userId"

  private val appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  val userId = "user-123"
  val submissionId: SubmissionId = SubmissionId("sub-001")


  private def authStub(allow: Boolean, ex: AuthorisationException = MissingBearerToken()): AuthConnector =
    new AuthConnector {

      override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
        if (allow) Future.successful(().asInstanceOf[A]) else Future.failed(ex)

    }

  val repo: UserAnswersRepository = appBuilder.injector().instanceOf[UserAnswersRepository]

  private def appWith(repo: UserAnswersRepository, auth: AuthConnector) =
    appBuilder
      .overrides(
        bind[UserAnswersRepository].toInstance(repo),
        bind[AuthConnector].toInstance(auth)
      )
      .build()

  private val userAnswers: UserAnswers = UserAnswers(userId, submissionId)

  private val sampleJson: JsValue = Json.toJson(userAnswers)


  "UserAnswersController" should {

    "POST /user-answers - return 204 NoContent for a valid payload" in {
      val application = appWith(repo, authStub(allow = true))

      running(application) {
        val request =
          FakeRequest(POST, saveUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(sampleJson)

        val result = route(application, request).value
        status(result) mustBe NO_CONTENT


        val stored: Option[UserAnswers] = await(repo.getUserAnswers(userId, submissionId))
        stored.value mustBe userAnswers

      }

      application.stop()
    }

    "POST /user-answers - return 400 BadRequest for invalid payload" in {
      val application = appWith(repo, authStub(allow = true))

      val badJson = Json.obj("invalid" -> "invalid")

      running(application) {
        val request =
          FakeRequest(POST, saveUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(badJson)

        val result = route(application, request).value
        status(result) mustBe BAD_REQUEST

      }

      application.stop()
    }

    "GET /user-answers/:userId/:submissionId - return 200 when the userAnswers exists" in {
      val application = appWith(repo, authStub(allow = true))

      running(application) {
        val request =
          FakeRequest(GET, retrieveUrl(userId, submissionId))


        await(repo.saveUserAnswers(userAnswers))
        val result = route(application, request).value
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(userAnswers)

      }

      application.stop()
    }

    "GET /user-answers/:userId/:submissionId - return 404 not found when no existing user answers are found" in {
      val application = appWith(repo, authStub(allow = true))
      
      val submissionId = SubmissionId("invalid")

      running(application) {
        val request =
          FakeRequest(GET, retrieveUrl(userId, submissionId))

        val result = route(application, request).value
        status(result) mustBe NOT_FOUND

      }

      application.stop()
    }

    "GET /user-answers/:userId- return a list of submissionIds" in {
      val application = appWith(repo, authStub(allow = true))

      running(application) {
        val request =
          FakeRequest(GET, retrieveSubmissionIdsUrl(userId))


        await(repo.saveUserAnswers(userAnswers))
        await(repo.saveUserAnswers(userAnswers.copy(submissionId = SubmissionId("sub-002"))))

        val result = route(application, request).value
        status(result) mustBe OK

        val expectedResult = await(repo.getSubmissionIds(userId))
        contentAsJson(result) mustBe Json.toJson(expectedResult)

      }

      application.stop()
    }

  }
}