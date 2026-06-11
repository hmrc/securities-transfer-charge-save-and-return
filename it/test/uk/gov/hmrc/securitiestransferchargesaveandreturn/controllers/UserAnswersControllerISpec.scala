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
import play.api.mvc.*
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.{GroupIdentifier, SubmissionId, UserAnswers, UserId}
import uk.gov.hmrc.securitiestransferchargesaveandreturn.repositories.UserAnswersRepository
import uk.gov.hmrc.securitiestransferchargesaveandreturn.support.AuthStub

import scala.concurrent.Future

class UserAnswersControllerISpec
  extends AnyWordSpec
    with Matchers
    with OptionValues
    with AuthStub {

  private val saveUrl: String = "/securities-transfer-charge-save-and-return/user-answers"

  private def retrieveUrl(submissionId: SubmissionId): String =
    s"/securities-transfer-charge-save-and-return/user-answers/$submissionId"

  private def retrieveSubmissionIdsUrl(userId: String): String =
    s"/securities-transfer-charge-save-and-return/user-answers/search/by-user?userId=$userId"

  private val appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  val userId: UserId = UserId("user-123")
  val groupIdentifier: GroupIdentifier = GroupIdentifier("group-123")
  val submissionId: SubmissionId = SubmissionId("sub-001")

  val repo: UserAnswersRepository = appBuilder.injector().instanceOf[UserAnswersRepository]

  private def appWith(repo: UserAnswersRepository, auth: AuthConnector) =
    appBuilder
      .overrides(
        bind[UserAnswersRepository].toInstance(repo),
        bind[AuthConnector].toInstance(auth)
      )
      .build()

  private val userAnswers: UserAnswers = UserAnswers(userId, groupIdentifier, submissionId)

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


        val stored: Option[UserAnswers] = await(repo.getUserAnswers(submissionId))
        stored.value.copy(lastUpdated = userAnswers.lastUpdated) mustBe userAnswers

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

    "GET /user-answers/:submissionId - return 200 when the userAnswers exists" in {
      val application = appWith(repo, authStub(allow = true))

      running(application) {
        val request =
          FakeRequest(GET, retrieveUrl(submissionId))


        await(repo.saveUserAnswers(userAnswers))
        val result = route(application, request).value
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(userAnswers)

      }

      application.stop()
    }

    "GET /user-answers/:submissionId - return 404 when no existing user answers are found" in {
      val application = appWith(repo, authStub(allow = true))

      val submissionId = SubmissionId("invalid")

      running(application) {
        val request =
          FakeRequest(GET, retrieveUrl(submissionId))

        val result = route(application, request).value
        status(result) mustBe NOT_FOUND
      }

      application.stop()
    }

    "GET /user-answers/search/by-user - return a list of submissionIds" in {
      val application = appWith(repo, authStub(allow = true))

      running(application) {
        val request =
          FakeRequest(GET, retrieveSubmissionIdsUrl(userId.value))


        await(repo.saveUserAnswers(userAnswers))
        await(repo.saveUserAnswers(userAnswers.copy(submissionId = SubmissionId("sub-002"))))

        val result = route(application, request).value
        status(result) mustBe OK

        val expectedResult = await(repo.getSubmissionIdsByUser(userId))
        contentAsJson(result) mustBe Json.toJson(expectedResult)

      }

      application.stop()
    }

    "GET /user-answers/:userId- return 200 status code with an empty list when no submissionIds are found" in {
      val application = appWith(repo, authStub(allow = true))

      val uid = "user4"
      val userId = UserId(uid)

      running(application) {
        val request =
          FakeRequest(GET, retrieveSubmissionIdsUrl(uid))


        val result = route(application, request).value
        status(result) mustBe OK

        val expectedResult = await(repo.getSubmissionIdsByUser(userId))
        expectedResult mustBe List.empty
        contentAsJson(result) mustBe Json.toJson(expectedResult)
      }

      application.stop()
    }

  }
}
