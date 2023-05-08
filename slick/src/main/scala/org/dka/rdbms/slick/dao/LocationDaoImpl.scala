package org.dka.rdbms.slick.dao

import org.dka.rdbms.common.dao.LocationDao
import org.dka.rdbms.common.model.components.{CountryID, ID, LocationAbbreviation, LocationName}
import org.dka.rdbms.common.model.item.Location
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

class LocationDaoImpl(override val db: Database) extends CrudDaoImpl[Location] with LocationDao {
  import LocationDaoImpl._

  //
  // crud IO operations
  //
  override protected val singleInsertIO: Location => DBIO[Int] = location => tableQuery += location
  override protected val multipleInsertIO: Seq[Location] => DBIO[Option[Int]] = locations => tableQuery ++= locations
  override protected val getIO: (ID, ExecutionContext) => DBIO[Option[Location]] = (id, ec) =>
    tableQuery.filter(_.id === id.value.toString).result.map(_.headOption)(ec)
  override protected val deletedIO: ID => DBIO[Int] = id => tableQuery.filter(_.id === id.value.toString).delete

  //
  // additional IO operations
  // needed to support AuthorDao
  //
}

object LocationDaoImpl {
  val tableQuery = TableQuery[LocationTable]

  class LocationTable(tag: Tag)
    extends Table[Location](
      tag,
      None, // schema is set at connection time rather than a compile time, see DBConfig notes
      "locations") {
    val id = column[String]("id", O.PrimaryKey) // This is the primary key column
    private val locationName = column[String]("location_name")
    private val locationAbbreviation = column[String]("location_abbreviation")
    private val countryID = column[String]("country_id")

    // Every table needs a * projection with the same type as the table's type parameter
    override def * = (id, locationName, locationAbbreviation, countryID) <> (fromDB, toDB)
  }

  //
  // conversions between db and model
  // the model is guaranteed valid,
  // the db is assumed valid because the data only come from the model
  //

  private type LocationTuple = (
    String, // id
    String, // location_name
    String, // location_abbreviation
    String // country_id
  )

  def fromDB(tuple: LocationTuple): Location = {
    val (id, locationName, locationAbbreviation, countryID) = tuple
    Location(
      ID.build(UUID.fromString(id)),
      locationName = LocationName.build(locationName),
      locationAbbreviation = LocationAbbreviation.build(locationAbbreviation),
      countryID = CountryID.build(UUID.fromString(countryID))
    )
  }

  def toDB(location: Location): Option[LocationTuple] = Some(
    location.id.value.toString,
    location.locationName.value,
    location.locationAbbreviation.value,
    location.countryID.value.toString
  )

}
