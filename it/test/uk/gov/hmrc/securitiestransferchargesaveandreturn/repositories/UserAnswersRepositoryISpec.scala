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

package uk.gov.hmrc.securitiestransferchargesaveandreturn.repositories

import org.bson.Document
import org.mongodb.scala.ObservableFuture
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.{GroupIdentifier, JourneyType, SubmissionId, UserAnswers, UserId}

class UserAnswersRepositoryISpec extends AnyWordSpec with GuiceOneAppPerSuite with ScalaFutures with Matchers with OptionValues {

  lazy val repo: UserAnswersRepositoryImpl =
    app.injector.instanceOf[UserAnswersRepositoryImpl]

  private def indexes: Seq[Document] =
    repo.collection.listIndexes[Document]().toFuture().futureValue

  "UserAnswersRepository indexes" should {

    "have a unique index on submissionId" in {
      val index = indexes.find(_.getString("name") == "submissionIdIdx").value

      index.getBoolean("unique") mustBe true
    }

    "have an index on userId" in {
      indexes.exists(_.getString("name") == "userIdIdx") mustBe true
    }

    "have an index on groupIdentifier" in {
      indexes.exists(_.getString("name") == "groupIdentifierIdx") mustBe true
    }

    "keep the TTL index on userAnswers.createdAt" in {
      val index = indexes.find(_.getString("name") == "createdAtIdx").value

      index.get("key", classOf[Document]).get("userAnswers.createdAt") mustBe Integer.valueOf(1)
      index.getInteger("expireAfterSeconds") mustBe 30 * 24 * 60 * 60
    }
  }

  "UserAnswersRepository.saveUserAnswers" should {

    "persist and return fileUploadReference" in {
      val submissionId = SubmissionId("STC-ISPEC-FILEREF")
      val userAnswers = UserAnswers(
        userId = UserId("user-fileref"),
        groupIdentifier = GroupIdentifier("group-fileref"),
        submissionId = submissionId,
        journeyType = JourneyType("stf"),
        fileUploadReference = Some("upload-ref-42")
      )

      repo.saveUserAnswers(userAnswers).futureValue
      repo.getUserAnswers(submissionId).futureValue.value.fileUploadReference mustBe Some("upload-ref-42")
    }
  }
}
