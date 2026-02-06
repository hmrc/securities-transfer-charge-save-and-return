/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.securitiestransferchargesaveandreturn.models.{SubmissionId, UserAnswers}
import uk.gov.hmrc.securitiestransferchargesaveandreturn.repositories.UserAnswersRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserAnswersController @Inject()(
                                       cc: ControllerComponents,
                                       userAnswersRepository: UserAnswersRepository,
                                       val authConnector: AuthConnector
                                     )(implicit ec: ExecutionContext)
  extends BackendController(cc) with AuthorisedFunctions {

  def save: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authorised() {
        request.body.validate[UserAnswers].fold(
          errors => Future.successful(BadRequest(JsError.toJson(errors))),
          userAnswers =>
            userAnswersRepository.saveUserAnswers(userAnswers).map(_ => NoContent)
        )
      }
  }

  def retrieve(userId: String, submissionId: SubmissionId): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      userAnswersRepository.getUserAnswers(userId, submissionId).map {
        case Some(userAnswers) => Ok(Json.toJson(userAnswers))
        case None => NotFound
      }
    }
  }

  def retrieveSubmissionIds(userId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      userAnswersRepository.getSubmissionIds(userId).map { userAnswers =>
        Ok(Json.toJson(userAnswers))
      }
    }
  }

}