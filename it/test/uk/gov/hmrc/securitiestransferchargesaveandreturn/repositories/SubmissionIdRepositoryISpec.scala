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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers.convertToStringShouldWrapperForVerb
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.SubmissionIdCounter
import org.scalatest.BeforeAndAfterEach
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.model.Filters

import scala.concurrent.{ExecutionContext, Future}


class SubmissionIdRepositoryISpec extends AnyWordSpec with GuiceOneAppPerSuite with ScalaFutures with Matchers with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  lazy val submissionIdRepo: SubmissionIdRepositoryImpl =
    app.injector.instanceOf[SubmissionIdRepositoryImpl]

  override def beforeEach(): Unit = {
    submissionIdRepo.collection
      .deleteMany(Filters.empty())
      .toFuture()
      .futureValue
  }

  "SubmissionIdRepository.nextId" should {

    "create the counter document if it does not exist" in {
      val result = submissionIdRepo.nextId().futureValue
      result.value mustBe "STC-000000001"

      val doc = submissionIdRepo.collection
        .find(Filters.equal("_id", SubmissionIdCounter.counterId))
        .head()
        .futureValue

      doc.current mustBe 1
    }

    "return STC-000000001 on first call" in {
      submissionIdRepo.nextId().futureValue.value mustBe "STC-000000001"
    }

    "increment sequentially" in {
      submissionIdRepo.nextId().futureValue
      submissionIdRepo.nextId().futureValue

      submissionIdRepo.nextId().futureValue.value mustBe "STC-000000003"
    }

    "wrap back to zero after max value" in {

      submissionIdRepo.collection.insertOne(
        SubmissionIdCounter(
          _id = SubmissionIdCounter.counterId,
          current = SubmissionIdCounter.maxValue
        )
      ).toFuture().futureValue

      submissionIdRepo.nextId().futureValue.value mustBe "STC-000000000"
    }

    "not produce duplicate IDs under concurrency" in {

      val futures = (1 to 100).map(_ => submissionIdRepo.nextId())

      val results = Future.sequence(futures).futureValue

      results.map(_.value).distinct.size mustBe 100
    }
  }
}
