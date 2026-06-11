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
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.{GroupIdentifier, SubmissionId, UserAnswers, UserId}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class UserAnswersDocument( id: String,
                                userId: UserId,
                                submissionId: SubmissionId,
                                userAnswers: UserAnswers)

object UserAnswersDocument {
  implicit val format: OFormat[UserAnswersDocument] = Json.format[UserAnswersDocument]
  def apply(userAnswers: UserAnswers): UserAnswersDocument = {
    UserAnswersDocument(
      userAnswers.submissionId.value,
      userAnswers.userId,
      userAnswers.submissionId,
      userAnswers
    )
  }
}

trait UserAnswersRepository:
  def getUserAnswers(submissionId: SubmissionId): Future[Option[UserAnswers]]
  def saveUserAnswers(userAnswers: UserAnswers): Future[Unit]
  def getSubmissionIdsByUser(userId: UserId): Future[Seq[SubmissionId]]
  def getSubmissionIdsByGroup(groupId: GroupIdentifier): Future[Seq[SubmissionId]]


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

  private def byUserId(userId: UserId): Bson = Filters.equal("userId", userId)
  private def byGroupId(groupId: GroupIdentifier): Bson = Filters.equal("groupIdentifier", groupId)
  private def bySubmissionId(submissionId: SubmissionId): Bson = Filters.equal("submissionId", submissionId.value)

  override def getUserAnswers( submissionId: SubmissionId): Future[Option[UserAnswers]] =
    collection
      .find(bySubmissionId(submissionId))
      .map(_.userAnswers)
      .headOption()

  override def getSubmissionIdsByUser(userId: UserId): Future[Seq[SubmissionId]] =
    collection
      .find(byUserId(userId))
      .map(_.submissionId)
      .toFuture()
      
  override def getSubmissionIdsByGroup(groupId: GroupIdentifier): Future[Seq[SubmissionId]] =
    collection
      .find(byGroupId(groupId))
      .map(_.submissionId)
      .toFuture()

  override def saveUserAnswers(userAnswers: UserAnswers): Future[Unit] = {
    collection
      .replaceOne(
        filter      = bySubmissionId(userAnswers.submissionId),
        replacement = UserAnswersDocument(userAnswers),
        options     = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
  }

}
