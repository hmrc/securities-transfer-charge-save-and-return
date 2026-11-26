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

package models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.{GroupIdentifier, JourneyType, SubmissionId, UserAnswers, UserId}

import java.time.Instant

class UserAnswersSpec extends AnyWordSpec with Matchers {

  private val createdAt = Instant.parse("2026-01-01T10:00:00Z")
  private val lastUpdated = Instant.parse("2026-01-01T11:00:00Z")

  private val base = UserAnswers(
    userId = UserId("user-1"),
    groupIdentifier = GroupIdentifier("group-1"),
    submissionId = SubmissionId("STC-000000001"),
    journeyType = JourneyType("stf"),
    createdAt = createdAt,
    lastUpdated = lastUpdated
  )

  "UserAnswers JSON" should {

    "round-trip fileUploadReference" in {
      val withRef = base.copy(fileUploadReference = Some("file-ref-123"))

      val json = Json.toJson(withRef)

      (json \ "fileUploadReference").as[String] mustBe "file-ref-123"
      json.as[UserAnswers].fileUploadReference mustBe Some("file-ref-123")
    }

    "omit fileUploadReference when not set" in {
      Json.toJson(base).as[JsObject].keys must not contain "fileUploadReference"
    }

    "read a document without fileUploadReference as None" in {
      val json = Json.toJson(base).as[JsObject] - "fileUploadReference"

      json.as[UserAnswers].fileUploadReference mustBe None
    }
  }
}
