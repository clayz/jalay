package core.mail

import collection.mutable.Set
import org.apache.commons.lang.StringUtils
import com.typesafe.plugin._
import play.api.Play.current
import core.common._

/**
 * Mail contact object, contains mail address and contact name.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class Contact(mail: String, name: String = "") {
  override def toString: String = if (StringUtils.isBlank(name)) this.mail else "%s <%s>".format(this.name, this.mail)
}

/**
 * All system contact list.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Contact {
  val Support = Contact(AppConfig.get("ca.mail.support"))
  val Register = Contact(AppConfig.get("ca.mail.register"))
}

/**
 * This class holds all email related info and provides basic mail functions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class Mail(var subject: Option[String] = None,
                var from: Option[Contact] = None,
                val toList: Set[Contact] = Set(),
                val ccList: Set[Contact] = Set(),
                val bccList: Set[Contact] = Set(),
                var content: Option[String] = None,
                var isHTML: Boolean = true) {

  /**
   * Other constructors.
   */
  def this(subject: String, from: Contact, to: Contact) = this(Some(subject), Some(from), Set(to))

  def this(subject: String, from: Contact, to: Contact, cc: Contact) = this(Some(subject), Some(from), Set(to), Set(cc))

  def this(subject: String, from: Contact, to: Contact, isHTML: Boolean) = this(Some(subject), Some(from), Set(to), Set(), Set(), None, isHTML)

  def this(subject: String, from: Contact, to: Contact, cc: Contact, isHTML: Boolean) = this(Some(subject), Some(from), Set(to), Set(cc), Set(), None, isHTML)

  def this(subject: String, from: Contact, to: Contact, cc: Contact, content: String) = this(Some(subject), Some(from), Set(to), Set(cc), Set(), Some(content))

  /**
   * Add mail body.
   *
   * @param text Mail content to be send.
   */
  def body(text: String) = this.content = Some(text)

  /**
   * Set mail subject.
   *
   * @param text Mail subject.
   */
  def setSubject(text: String) = this.subject = Some(text)

  /**
   * Set mail from address.
   *
   * @param contact Mail send from.
   */
  def setFrom(contact: Contact) = this.from = Some(contact)

  /**
   * Add mail recipients.
   *
   * @param contact Send to contacts.
   */
  def addTo(contact: Contact*) = this.toList ++= contact

  /**
   * Add mail CC contacts.
   *
   * @param contact CC contacts to send.
   */
  def addCc(contact: Contact*) = this.ccList ++= contact

  /**
   * Add mail BCC contacts.
   *
   * @param contact BCC contacts to send.
   */
  def addBcc(contact: Contact*) = this.bccList ++= contact

  /**
   * Check all mail fields then send to all TO, CC, BCC contacts.
   */
  def send: Boolean =
    try {
      Log.debug("Sending mail start......")
      Log.debug("Subject: " + this.subject.getOrElse(throw new MailException("Missing mail subject.")))
      Log.debug("From: " + this.from.getOrElse(throw new MailException("Missing mail from address.")).toString)
      Log.debug("To: " + this.toList.toString)
      Log.debug("Cc: " + this.ccList.toString)
      Log.debug("Bcc: " + this.bccList.toString)
      Log.debug("Body: " + this.content.getOrElse(throw new MailException("Missing mail body.")))

      if (AppEnv.isLocal) {
        Log.debug("Local environment, mail send escaped.")
        return true
      }

      val mail = use[MailerPlugin].email

      // set mail header info
      mail.setSubject(this.subject.get)
      mail.addFrom(this.from.get.toString)
      mail.addRecipient((for (to <- this.toList.toSeq) yield to.toString): _*)
      mail.addCc((for (cc <- this.ccList.toSeq) yield cc.toString): _*)
      mail.addBcc((for (bcc <- this.bccList.toSeq) yield bcc.toString): _*)

      // send email
      if (isHTML) mail.sendHtml(this.content.get) else mail.send(this.content.get)
      Log.debug("Sending mail finish......")
      true
    } catch {
      case e: MailException => throw e
      case e: Throwable =>
        Log.error("Send mail failed.", e)

        Log.info("Subject: " + this.subject.get)
        Log.info("From: " + this.from.get.toString)
        Log.info("To: " + this.toList.toString)
        Log.info("Cc: " + this.ccList.toString)
        Log.info("Bcc: " + this.bccList.toString)
        Log.info("Body: " + this.content.get)

        false
    }
}