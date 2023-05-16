package org.dka.rdbms.slick.dao

import org.dka.rdbms.common.dao.CountryDao
import org.dka.rdbms.common.model.fields.{CountryAbbreviation, CountryName, ID, Version}
import org.dka.rdbms.common.model.item.Country
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

class CountryDaoImpl(override val db: Database) extends CrudDaoImpl[Country] with CountryDao {
  import CountryDaoImpl._

  //
  // crud IO operations
  //
  override protected val singleCreateIO: Country => DBIO[Int] = country => tableQuery += country
  override protected val multipleCreateIO: Seq[Country] => DBIO[Option[Int]] = countries => tableQuery ++= countries
  override protected val getIO: (ID, ExecutionContext) => DBIO[Option[Country]] = (id, ec) =>
    tableQuery.filter(_.id === id.value.toString).result.map(_.headOption)(ec)
  override protected val deletedIO: ID => DBIO[Int] = id => tableQuery.filter(_.id === id.value.toString).delete

  override protected val updateAction: (Country, ExecutionContext) => DBIO[Country] = (item, ec) => ???
//  override def updateAction(item: Country): PostgresProfile.ProfileAction[Int, NoStream, Effect.Write] = {
//    val query = tableQuery.filter(_.version === item.version.value).map(ct => (ct.version, ct.countryName, ct.countryAbbreviation))
//    query.update((item.version.value + 1, item.countryName.value, item.countryAbbreviation.value))
//  }

  //
  // additional IO operations
  // needed to support CountryDao
  //
}

object CountryDaoImpl {
  val tableQuery = TableQuery[CountryTable]

  class CountryTable(tag: Tag)
    extends Table[Country](
      tag,
      None, // schema is set at connection time rather than a compile time, see DBConfig notes
      "countries") {
    val id = column[String]("id", O.PrimaryKey) // This is the primary key column
    val version = column[Int]("version")
    val countryName = column[String]("country_name")
    val countryAbbreviation = column[String]("country_abbreviation")

    // Every table needs a * projection with the same type as the table's type parameter
    override def * = (id, version, countryName, countryAbbreviation) <> (fromDB, toDB)
  }

  //
  // conversions between db and model
  // the model is guaranteed valid,
  // the db is assumed valid because the data only come from the model
  //

  private type CountryTuple = (
    String, // id
    Int, // version
    String, // country_name
    String // country_abbreviation
  )

  def fromDB(tuple: CountryTuple): Country = {
    val (id, version, countryName, countryAbbreviation) = tuple
    Country(
      ID.build(UUID.fromString(id)),
      Version.build(version),
      countryName = CountryName.build(countryName),
      countryAbbreviation = CountryAbbreviation.build(countryAbbreviation)
    )
  }

  def toDB(country: Country): Option[CountryTuple] = Some(
    country.id.value.toString,
    country.version.value,
    country.countryName.value,
    country.countryAbbreviation.value
  )

}
