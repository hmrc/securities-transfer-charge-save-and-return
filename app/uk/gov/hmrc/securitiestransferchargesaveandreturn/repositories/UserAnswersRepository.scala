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
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.{SubmissionId, UserAnswers}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class UserAnswersDocument( id: String,
                                userId: String,
                                submissionId: SubmissionId,
                                userAnswers: UserAnswers)

object UserAnswersDocument {
  implicit val format: OFormat[UserAnswersDocument] = Json.format[UserAnswersDocument]
  def apply(userAnswers: UserAnswers): UserAnswersDocument = {
    UserAnswersDocument(
      s"${userAnswers.userId}:${userAnswers.submissionId}",
      userAnswers.userId,
      userAnswers.submissionId,
      userAnswers
    )
  }
}

trait UserAnswersRepository:
  def getUserAnswers(userId: String, submissionId: SubmissionId): Future[Option[UserAnswers]]
  def saveUserAnswers(userAnswers: UserAnswers): Future[Unit]
  def getSubmissionIds(userId: String): Future[Seq[SubmissionId]]

@Singleton
class UserAnswersRepositoryImpl @Inject()(mongoComponent: MongoComponent,
                                          appConfig: AppConfig)
                                         (implicit ec: ExecutionContext)
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

  private def byUserId(userId: String): Bson = Filters.equal("userId", userId)

  private def bySubmissionId(userId: String, submissionId: SubmissionId): Bson =
    Filters.and(
      Filters.equal("userId", userId),
      Filters.equal("submissionId", submissionId.value)
    )

  override def getUserAnswers(userId: String,
                              submissionId: SubmissionId): Future[Option[UserAnswers]] =
    collection
      .find(bySubmissionId(userId, submissionId))
      .map(_.userAnswers)
      .headOption()

  override def getSubmissionIds(userId: String): Future[Seq[SubmissionId]] =
    collection
      .find(byUserId(userId))
      .map(_.submissionId)
      .toFuture()

  override def saveUserAnswers(userAnswers: UserAnswers): Future[Unit] = {
    collection
      .replaceOne(
        filter      = bySubmissionId(userAnswers.userId, userAnswers.submissionId),
        replacement = UserAnswersDocument(userAnswers),
        options     = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
  }

}
