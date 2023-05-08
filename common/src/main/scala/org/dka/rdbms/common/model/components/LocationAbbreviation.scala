package org.dka.rdbms.common.model.components

import org.dka.rdbms.common.model.item.Item
import org.dka.rdbms.common.model.validation.StringLengthValidation

/**
 * country requirements:
 *   - can not be more than 40
 */
final case class LocationAbbreviation private (override val value: String) extends Item[String]

object LocationAbbreviation extends StringLengthValidation[LocationAbbreviation] {
  override val maxLength = 40
  override val minLength = 1
  override val fieldName: String = "country_abbreviation"

  override def build(a: String): LocationAbbreviation = new LocationAbbreviation(a)
}
