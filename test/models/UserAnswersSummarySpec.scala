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
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.{JourneyType, SubmissionId, UserAnswersSummary}

import java.time.Instant

class UserAnswersSummarySpec extends AnyWordSpec with Matchers {

  "UserAnswersSummary" should {

    "serialize to JSON" in {
      val summary = UserAnswersSummary(
        submissionId = SubmissionId("sub-001"),
        maybeAgentReference = Some("agent-ref"),
        journeyType = JourneyType("STF"),
        createdAt = Instant.parse("2026-01-01T10:00:00Z"),
        lastUpdated = Instant.parse("2026-01-01T11:00:00Z")
      )

      val json = Json.toJson(summary)

      json shouldBe Json.obj(
        "submissionId"       -> "sub-001",
        "maybeAgentReference" -> "agent-ref",
        "journeyType"        -> "STF",
        "createdAt"          -> "2026-01-01T10:00:00Z",
        "lastUpdated"        -> "2026-01-01T11:00:00Z"
      )
    }

    "deserialize from JSON" in {
      val json = Json.obj(
        "submissionId"        -> "sub-001",
        "maybeAgentReference" -> "agent-ref",
        "journeyType"        -> "STF",
        "createdAt"           -> "2026-01-01T10:00:00Z",
        "lastUpdated"         -> "2026-01-01T11:00:00Z"
      )

      json.as[UserAnswersSummary] shouldBe UserAnswersSummary(
        submissionId = SubmissionId("sub-001"),
        maybeAgentReference = Some("agent-ref"),
        journeyType = JourneyType("STF"),
        createdAt = Instant.parse("2026-01-01T10:00:00Z"),
        lastUpdated = Instant.parse("2026-01-01T11:00:00Z")
      )
    }
  }
}