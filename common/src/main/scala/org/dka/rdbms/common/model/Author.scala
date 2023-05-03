package org.dka.rdbms.common.model

import cats.implicits._
import cats.data.Validated._
import io.circe._
import Validation._

final case class Author(
  id: ID,
  lastName: LastName,
  firstName: FirstName,
  phone: Option[Phone],
  address: Option[Address],
  city: Option[City],
  state: Option[State],
  zip: Option[Zip]) {}

object Author {
  implicit val encodeAuthor: Encoder[Author] = (a: Author) => {
    val objects = List(
      Some(ID.toJsonLine(a.id)),
      Some(LastName.toJsonLine(a.lastName)),
      Some(FirstName.toJsonLine(a.firstName)),
      Phone.toJsonLine(a.phone),
      Address.toJsonLine(a.address),
      City.toJsonLine(a.city),
      State.toJsonLine(a.state),
      Zip.toJsonLine(a.zip)
    ).flatten // filter out the None, i.e. only needed lines
    Json.obj(objects: _*)
  }

  implicit val decodeAuthor: Decoder[Author] = (c: HCursor) => {
    val result: ValidationErrorsOr[Author] =
      (
        ID.fromJsonLine(c),
        LastName.fromJsonLine(c),
        FirstName.fromJsonLine(c),
        Phone.fromOptionalJsonLine(c),
        Address.fromOptionalJsonLine(c),
        City.fromOptionalJsonLine(c),
        State.fromOptionalJsonLine(c),
        Zip.fromOptionalJsonLine(c)
      ).mapN(Author.apply)
    result match {
      case Invalid(errors) =>
        Left(DecodingFailure(errors, Nil))
      case Valid(author) => Right(author)
    }
  }
}
