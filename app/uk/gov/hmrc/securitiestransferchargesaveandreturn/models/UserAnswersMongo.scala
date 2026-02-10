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

package uk.gov.hmrc.securitiestransferchargesaveandreturn.models

import play.api.libs.json.*
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class UserAnswersMongo(
                       submissionId: SubmissionId,
                       data: JsObject = Json.obj(),
                       lastUpdated: Instant = Instant.now
                      )

object UserAnswersMongo {
  val reads: Reads[UserAnswersMongo] = {

    import play.api.libs.functional.syntax.*

    (
        (__ \ "submissionId").read[SubmissionId] and
        (__ \ "data").read[JsObject] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      )(UserAnswersMongo.apply _)
  }

  val writes: OWrites[UserAnswersMongo] = {

    import play.api.libs.functional.syntax.*

    (
        (__ \ "submissionId").write[SubmissionId] and
        (__ \ "data").write[JsObject] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      )(ua => (ua.submissionId, ua.data, ua.lastUpdated))
  }

  implicit val format: OFormat[UserAnswersMongo] = OFormat(reads, writes)
}