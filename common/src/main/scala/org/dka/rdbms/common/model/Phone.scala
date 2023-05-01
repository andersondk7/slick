package org.dka.rdbms.common.model

/**
 * phone requirements:
 *   - must be 12 characters
 */
final case class Phone private (override val value: String) extends Item[String]

object Phone extends StringValidated[Phone] {
  override val maxLength = 12
  override val minLength = 12
  override val fieldName: String = "phone"

  override def build(s: String): Phone = new Phone(s)
}
