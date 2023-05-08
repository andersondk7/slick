package org.dka.rdbms.common.model.components

import org.dka.rdbms.common.model.item.Item
import org.dka.rdbms.common.model.validation.StringLengthValidation

/**
 * titleName requirements:
 *   - can't be empty
 *   - can not be more than 30
 */
final case class Title private (override val value: String) extends Item[String]

object Title extends StringLengthValidation[Title] {
  override val maxLength = 200
  override val minLength = 1
  override val fieldName: String = "title"

  override def build(tn: String): Title = new Title(tn)
}
