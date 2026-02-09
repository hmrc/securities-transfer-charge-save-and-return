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
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.securitiestransferchargesaveandreturn.config.AppConfig
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.{SubmissionId, UserAnswers}

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


trait UserAnswersRepository {
  def getUserAnswers(userId: String, submissionId: SubmissionId): Future[Option[UserAnswers]]

  def saveUserAnswers(userAnswers: UserAnswers): Future[Unit]

  def getSubmissionIds(userId: String): Future[Seq[SubmissionId]]
}


@Singleton
class UserAnswersRepositoryImpl @Inject()(mongoComponent: MongoComponent,
                                          appConfig: AppConfig
                                         )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[UserAnswers](
    collectionName = "userAnswers",
    mongoComponent = mongoComponent,
    domainFormat = UserAnswers.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.timeToLive, TimeUnit.DAYS),
      )
    )
  ) with UserAnswersRepository {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat


  private def byId(id: String): Bson = Filters.equal("userId", id)


  override def getUserAnswers(
                               userId: String,
                               submissionId: SubmissionId
                             ): Future[Option[UserAnswers]] =
    collection
      .find(
        Filters.and(
          byId(userId),
          Filters.eq("submissionId", submissionId.value)
        )
      )
      .headOption()

  override def getSubmissionIds(userId: String): Future[Seq[SubmissionId]] =
    collection.find(byId(userId)).map(_.submissionId).toFuture()

  override def saveUserAnswers(userAnswers: UserAnswers): Future[Unit] = {
    collection
      .replaceOne(
        filter =
          Filters.and(
            Filters.equal("userId", userAnswers.userId),
            Filters.equal("submissionId", userAnswers.submissionId.value)
          )
        ,
        replacement = userAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
  }
}
