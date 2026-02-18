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

import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, ReturnDocument, Updates}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.SubmissionId
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.SubmissionIdCounter

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

trait SubmissionIdRepository {
  def nextId(): Future[SubmissionId]
}

@Singleton
class SubmissionIdRepositoryImpl @Inject()(
                                            mongoComponent: MongoComponent
                                          )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[SubmissionIdCounter](
    collectionName = "submissionIdCounter",
    mongoComponent = mongoComponent,
    domainFormat   = SubmissionIdCounter.format,
    indexes        = Seq()
  )
    with SubmissionIdRepository {

  import SubmissionIdCounter._

  override def nextId(): Future[SubmissionId] = {

    val update = Updates.inc("current", 1) // increases the current field by one when called

    collection
      .findOneAndUpdate(                  // findOneAndUpdate guarantees atomicity, ive included a test which checks this (last test in ISpec)
        Filters.equal("_id", counterId),  // target this document { _id: "submissionIdCOunter"}
        update,                           // increment counter by 1
        FindOneAndUpdateOptions()
          .upsert(true)                   // if document doesnt exist, create it
          .returnDocument(ReturnDocument.AFTER)   // return the document after incrementing it
      )
      .toFuture()
      .map { counter =>

        val wrappedValue =
          if (counter.current > maxValue) 0L      // wrap check
          else counter.current
        
        if (counter.current > maxValue) {
          collection.updateOne(
            Filters.equal("_id", counterId),
            Updates.set("current", 0)
          ).toFuture()
        }

        SubmissionId(f"STC-$wrappedValue%09d")
      }
  }
}



