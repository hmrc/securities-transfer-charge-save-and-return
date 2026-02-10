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

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.securitiestransferchargesaveandreturn.config.AppConfig
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.{SubmissionId, UserAnswers, UserAnswersMongo}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class UserAnswersDocument(
                                userAnswers: Seq[UserAnswersMongo]
                              )

object UserAnswersDocument {
  implicit val format: OFormat[UserAnswersDocument] = Json.format[UserAnswersDocument]
}

trait UserAnswersRepository {
  def getUserAnswers(userId: String, submissionId: SubmissionId): Future[Option[UserAnswers]]

  def saveUserAnswers(userAnswers: UserAnswers): Future[Unit]

  def getSubmissionIds(userId: String): Future[Seq[SubmissionId]]
}


@Singleton
class UserAnswersRepositoryImpl @Inject()(
                                           mongoComponent: MongoComponent,
                                           appConfig: AppConfig
                                         )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[UserAnswersDocument](
    collectionName = "userAnswers",
    mongoComponent = mongoComponent,
    domainFormat = UserAnswersDocument.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.timeToLive, TimeUnit.DAYS)
      )
    )
  ) with UserAnswersRepository {


  private def byId(id: String): Bson = Filters.equal("_id", id)

  override def getUserAnswers(
                               userId: String,
                               submissionId: SubmissionId
                             ): Future[Option[UserAnswers]] = {
    collection
      .find(byId(userId))
      .headOption()
      .map(_.flatMap { doc =>
        doc.userAnswers.find(_.submissionId == submissionId).map { uaMongo =>
          UserAnswers(
            userId = userId,
            submissionId = uaMongo.submissionId,
            data = uaMongo.data,
            lastUpdated = uaMongo.lastUpdated
          )
        }
      })
  }


  override def getSubmissionIds(userId: String): Future[Seq[SubmissionId]] =
    collection
      .find(byId(userId))
      .headOption()
      .map(_.map(_.userAnswers.map(_.submissionId)).getOrElse(Seq.empty))

  override def saveUserAnswers(userAnswers: UserAnswers): Future[Unit] = {
    val filter = byId(userAnswers.userId)

    val mongoUserAnswer = UserAnswersMongo(
      submissionId = userAnswers.submissionId,
      data = userAnswers.data,
      lastUpdated = userAnswers.lastUpdated
    )

    collection
      .find(filter)
      .headOption()
      .flatMap { maybeDoc =>
        val updatedAnswers = maybeDoc match {
          case Some(doc) =>
            doc.userAnswers.filterNot(_.submissionId == mongoUserAnswer.submissionId) :+ mongoUserAnswer
          case None =>
            Seq(mongoUserAnswer)
        }

        val updatedDoc = UserAnswersDocument(
          userAnswers = updatedAnswers
        )

        collection
          .replaceOne(filter, updatedDoc, ReplaceOptions().upsert(true))
          .toFuture()
          .map(_ => ())
      }
  }
}