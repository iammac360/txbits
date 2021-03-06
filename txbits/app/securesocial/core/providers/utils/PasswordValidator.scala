/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
 *
 */
package securesocial.core.providers.utils

import play.api.{ Play, Plugin, Application }
import play.api.i18n.Messages
import play.api.data.validation.{ Invalid, Valid, Constraint }

object PasswordValidator {
  import PasswordValidator._

  val validator = Constraint[String](Some("Password complexity constraint"), "") {
    case password: String if password.length >= 12 => Valid
    case password => Invalid(Messages("securesocial.signup.invalidPassword", 12))
  }
}
