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

import securesocial.core.SocialUser
import play.api.{ Play, Logger }
import Play.current
import play.api.libs.concurrent.Akka
import play.api.mvc.RequestHeader
import play.api.i18n.Messages
import play.twirl.api.{ Html, Txt }
import controllers.SecureSocialTemplates
import org.apache.commons.mail.{ DefaultAuthenticator, SimpleEmail, MultiPartEmail, EmailAttachment }
import java.io.File
import javax.mail.internet.InternetAddress

/**
 * A helper class to send email notifications
 */
object Mailer {
  val fromAddress = current.configuration.getString("smtp.from").get
  val AlreadyRegisteredSubject = "mails.sendAlreadyRegisteredEmail.subject"
  val SignUpEmailSubject = "mails.sendSignUpEmail.subject"
  val WelcomeEmailSubject = "mails.welcomeEmail.subject"
  val PasswordResetSubject = "mails.passwordResetEmail.subject"
  val UnknownEmailNoticeSubject = "mails.unknownEmail.subject"
  val PasswordResetOkSubject = "mails.passwordResetOk.subject"

  def sendAlreadyRegisteredEmail(user: SocialUser)(implicit request: RequestHeader) {
    val txtAndHtml = SecureSocialTemplates.getAlreadyRegisteredEmail(user)
    sendEmail(Messages(AlreadyRegisteredSubject), user.email, txtAndHtml)

  }

  def sendSignUpEmail(to: String, token: String)(implicit request: RequestHeader) {
    val txtAndHtml = SecureSocialTemplates.getSignUpEmail(token)
    sendEmail(Messages(SignUpEmailSubject), to, txtAndHtml)
  }

  def sendWelcomeEmail(user: SocialUser)(implicit request: RequestHeader) {
    val txtAndHtml = SecureSocialTemplates.getWelcomeEmail(user)
    sendEmail(Messages(WelcomeEmailSubject), user.email, txtAndHtml)

  }

  def sendPasswordResetEmail(user: SocialUser, token: String)(implicit request: RequestHeader) {
    val txtAndHtml = SecureSocialTemplates.getSendPasswordResetEmail(user, token)
    sendEmail(Messages(PasswordResetSubject), user.email, txtAndHtml)
  }

  def sendPasswordChangedNotice(user: SocialUser)(implicit request: RequestHeader) {
    val txtAndHtml = SecureSocialTemplates.getPasswordChangedNoticeEmail(user)
    sendEmail(Messages(PasswordResetOkSubject), user.email, txtAndHtml)
  }

  private def sendEmail(subject: String, recipient: String, body: (Option[Txt], Option[Html])) {
    import com.typesafe.plugin._
    import scala.concurrent.duration._
    import play.api.libs.concurrent.Execution.Implicits._

    if (Logger.isDebugEnabled) {
      Logger.debug("[securesocial] sending email to %s".format(recipient))
    }

    Akka.system.scheduler.scheduleOnce(1.seconds) {
      val smtpHost = Play.current.configuration.getString("smtp.host").getOrElse(throw new RuntimeException("smtp.host needs to be set in application.conf in order to use this plugin (or set smtp.mock to true)"))
      val smtpPort = Play.current.configuration.getInt("smtp.port").getOrElse(25)
      val smtpSsl = Play.current.configuration.getBoolean("smtp.ssl").getOrElse(false)
      val smtpUser = Play.current.configuration.getString("smtp.user").get
      val smtpPassword = Play.current.configuration.getString("smtp.password").get
      val smtpLocalhost = current.configuration.getString("smtp.localhost").get
      val email = new SimpleEmail()
      email.setMsg(body._1.get.toString())
      email.setHostName(smtpLocalhost)
      //TODO: move this somewhere better
      System.setProperty("mail.smtp.localhost", current.configuration.getString("smtp.localhost").get)
      email.setCharset("utf-8")
      email.setSubject(subject)
      setAddress(fromAddress) { (address, name) => email.setFrom(address, name) }
      email.addTo(recipient, null)
      email.setHostName(smtpHost)
      email.setSmtpPort(smtpPort)
      email.setSSLOnConnect(smtpSsl)
      email.setAuthentication(smtpUser, smtpPassword)
      email.send
    }
  }

  /**
   * Extracts an email address from the given string and passes to the enclosed method.
   * https://github.com/typesafehub/play-plugins/blob/master/mailer/src/main/scala/com/typesafe/plugin/MailerPlugin.scala
   *
   * @param emailAddress
   * @param setter
   */
  private def setAddress(emailAddress: String)(setter: (String, String) => Unit) = {

    if (emailAddress != null) {
      try {
        val iAddress = new InternetAddress(emailAddress)
        val address = iAddress.getAddress
        val name = iAddress.getPersonal

        setter(address, name)
      } catch {
        case e: Exception =>
          setter(emailAddress, null)
      }
    }
  }

  def sendEmailWithFile(subject: String, recipient: String, body: String, attachment: EmailAttachment) {
    import com.typesafe.plugin._
    import scala.concurrent.duration._
    import play.api.libs.concurrent.Execution.Implicits._

    if (Logger.isDebugEnabled) {
      Logger.debug("[securesocial] sending email to %s".format(recipient))
      Logger.debug("[securesocial] mail = [%s]".format(body))
    }

    Akka.system.scheduler.scheduleOnce(1.seconds) {
      // we can't use the plugin easily with multipart emails
      val email = new MultiPartEmail
      email.setHostName(current.configuration.getString("smtp.host").get)
      //TODO: move this somewhere better
      System.setProperty("mail.smtp.localhost", current.configuration.getString("smtp.localhost").get)
      email.attach(attachment)
      email.setSubject(subject)
      email.addTo(recipient)
      email.setBoolHasAttachments(true)
      email.setSmtpPort(current.configuration.getInt("smtp.port").getOrElse(25))
      email.setSSLOnConnect(current.configuration.getBoolean("smtp.ssl").get)
      email.setAuthentication(current.configuration.getString("smtp.user").get, current.configuration.getString("smtp.password").get)
      setAddress(fromAddress) { (address, name) => email.setFrom(address, name) }
      email.setMsg(body)
      email.send()
      new File(attachment.getPath).delete()
    }
  }
}
