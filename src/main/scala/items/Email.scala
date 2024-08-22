package de.unruh.quickfind
package items

import core.{LeafItem, SVGImage, ScalableImage, SnippetPreviewItem}

import weka.classifiers.functions.SGDText
import weka.core.{Attribute, DenseInstance, Instances, SerializationHelper}

import java.util
import scala.jdk.CollectionConverters.SeqHasAsJava

class Email(address: String, preview: Option[(String,String,String)])
  extends SnippetPreviewItem(preview), LeafItem {
  override def title: String = address

  override def toString: String = s"[Email: $address]"
  
  override def defaultAction(): Unit =
    import sys.process._
    Seq("thunderbird", "-compose", s"to=$address").run()

  override def icon: ScalableImage = Email.icon
}

object Email {
  /**
   * Model for the decision made by [[isMessageId]]
   *
   * Classifier options: `weka.classifiers.functions.SGDText -F 0 -L 0.01 -R 1.0E-4 -E 500 -P 0 -M 3.0 -min-coeff 0.001 -norm 1.0 -lnorm 2.0 -stopwords-handler weka.core.stopwords.Null -tokenizer "weka.core.tokenizers.CharacterNGramTokenizer -max 2 -min 1" -stemmer weka.core.stemmers.NullStemmer -S 1`.
   * Trained on 14761 message-ids and 14760 email addresses.
   * Attribute `type` can be `messageid` or `address`.
   * Attribute `data` is the string to classify.
   *
   * Training data has this format:
   * {{{
   * @relation message
   * @attribute type {messageid,address}
   * @attribute data string
   * @data
   * messageid, '...@...'
   * address, '...@...'
   * }}}
   * (many more of the last two)
   *
   * This training data was generated by this Python code:
   * {{{
   * #!/usr/bin/python
   *
   * import os, email, re
   *
   * errors = 0
   * message_ids = set()
   * sender_emails = set()
   * count_addr = 0
   * count_mid = 0
   * output_file = None
   *
   * def extract_email_info(file_path):
   *     global errors, count_mid, count_addr
   *     with open(file_path, 'r') as file:
   *         try:
   *             msg = email.message_from_file(file)
   *             message_id = msg.get('Message-ID')
   *             sender_email = msg.get('From')
   *             if sender_email:
   *                 sender_email = sender_email.strip()
   *                 m = re.match('.*<([^<>]+)>', sender_email)
   *                 if m:
   *                     sender_email = m.group(1)
   *                     if '\n' not in sender_email and sender_email not in sender_emails:
   *                         count_addr += 1
   *                         print(f"address, '{sender_email}'", file=output_file)
   *                     sender_emails.add(sender_email)
   *             if message_id and count_addr >= count_mid:
   *                 message_id = message_id.strip().strip('<>')
   *                 if '\n' not in message_id and message_id not in message_ids:
   *                     count_mid += 1
   *                     print(f"messageid, '{message_id}'", file=output_file)
   *                 message_ids.add(message_id)
   *         except UnicodeDecodeError:
   *             errors += 1
   *
   * def scan_all():
   *     global output_file
   *     output_file = open("data.arff", 'wt')
   *     print("@relation message", file=output_file)
   *     print("@attribute type {messageid,address}", file=output_file)
   *     print("@attribute data string", file=output_file)
   *     print("@data", file=output_file)
   *
   *     for root, _, files in os.walk("."):
   *         for file in files:
   *             file_path = os.path.join(root, file)
   *             extract_email_info(file_path)
   *
   *     print(f"{errors} errors")
   *
   * scan_all()
   * }}}
   */
  private lazy val model = {
    val url = getClass.getResource("/ml/email-addr-vs-message-id.model")
    assert(url != null)
    val obj = SerializationHelper.read(url.openStream())
    obj.asInstanceOf[SGDText]
  }

  /** (Empty) dataset matching [[model]]. */
  private lazy val dataset = {
    val attribType = Attribute("type", Seq("messageid", "address").asJava)
    val attribData = Attribute("data", true)
    val dataset = Instances("dataset", util.ArrayList(Seq(attribType, attribData).asJava), 1)
    dataset.setClassIndex(0)
    dataset
  }

  /** Guesses whether a given string is a message-id or an email address */
  def isMessageId(address: String): Boolean = {
    val instance = DenseInstance(2)
    instance.setDataset(dataset)
    instance.setClassMissing()
    instance.setMissing(0)
    instance.setValue(1, address)
    val classification = model.synchronized(model.classifyInstance(instance))
    instance.setClassValue(classification)
    classification match
      case 0.0 => true
      case 1.0 => false
      case _ => assert(false)
  }

  def icon: ScalableImage = SVGImage.fromResource("/icons/at-sign-svgrepo-com.svg")
}
