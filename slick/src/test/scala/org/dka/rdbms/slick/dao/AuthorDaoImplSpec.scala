package org.dka.rdbms.slick.dao

import com.typesafe.scalalogging.Logger
import org.dka.rdbms.TearDownException
import org.dka.rdbms.common.model.{Address, Author, City, FirstName, ID, LastName, Phone, State, Zip}
import AuthorDaoImplSpec._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Success, Try}

class AuthorDaoImplSpec extends AnyFunSpec with DBTestRunner with Matchers {
  // for a test, this is fine ...
  implicit private val ec: ExecutionContext = ExecutionContext.global
  private val logger = Logger(getClass.getName)
  val delay: FiniteDuration = 10.seconds

  describe("conversion to/from db") {
    it("should convert from domain to db") {
      AuthorDaoImpl.toDB(mt) match {
        case None => fail(s"could not convert $mt")
        case Some((id, last, first, phone, address, city, state, zip)) =>
          id shouldBe mt.id.value
          last shouldBe mt.lastName.value
          first shouldBe mt.firstName.value
          phone shouldBe mt.phone.map(_.value)
          address shouldBe mt.address.map(_.value)
          city shouldBe mt.city.map(_.value)
          state shouldBe mt.state.map(_.value)
          zip shouldBe mt.zip.map(_.value)
      }
    }
    it("should convert from db to domain") {
      val db = (
        mt.id.value,
        mt.lastName.value,
        mt.firstName.value,
        mt.phone.map(_.value),
        mt.address.map(_.value),
        mt.city.map(_.value),
        mt.state.map(_.value),
        mt.zip.map(_.value)
      )
      val converted = AuthorDaoImpl.fromDB(db)
      converted shouldBe mt
    }
  }

  describe("populating") {
    it("should add an author") {
      val result = withDB(
        setup = noSetup,
        test = factory =>
          Try {
            Await.result(factory.authorsDao.create(ja), delay) match {
              case Left(e) => fail(e)
              case Right(author) =>
                logger.debug(s"attempting to insert author.id: ${ja.id}")
                author.id shouldBe ja.id
            }
          },
        tearDown = factory => deleteAuthor(ja.id)(factory, ec)
      )
      result.setupResult.failure shouldBe None
      result.tearDownResult.failure shouldBe None
      result.testResult.value
    }
    it("should add multiple authors") {
      val result = withDB(
        setup = noSetup,
        test = factory =>
          Try {
            val added = Future
              .sequence(multipleAuthors.map(id => factory.authorsDao.create(id)))
              .map(_.partitionMap(identity))
            val (errors, _) = Await.result(added, delay)
            if (errors.nonEmpty) throw errors.head
            // todo, need to be able to combine errors...
            else succeed
          },
        tearDown = factory => {
          val deleted = Future
            .sequence(multipleAuthors.map(author => factory.authorsDao.delete(author.id)))
            .map(_.partitionMap(identity))
          val (errors, _) = Await.result(deleted, delay)
          if (errors.nonEmpty) throw errors.head
          // todo, need to be able to combine errors...
          else Success()
        }
      )
      result.setupResult.failure shouldBe None
      result.tearDownResult.failure match {
        case Some(t) => t.printStackTrace()
        case None => println(s"no failures here")
      }
      result.tearDownResult.failure shouldBe None
      result.testResult.value
    }
    it("should find a specific author") {
      val result = withDB(
        setup = factory => loadAuthor(eh)(factory, ec),
        test = factory =>
          Try {
            Await.result(factory.authorsDao.read(eh.id), delay) match {
              case Left(e) => fail(e)
              case Right(opt) => opt.fold(fail(s"did not find $jm"))(author => author shouldBe jm)
            }
          },
        tearDown = factory => deleteAuthor(eh.id)(factory, ec)
      )
      result.setupResult.failure shouldBe None
      result.tearDownResult.failure shouldBe None
      result.testResult.value
    }
  }

  private def loadAuthor(author: Author)(implicit factory: DaoFactory, ec: ExecutionContext): Try[Unit] = Try {
    Await.result(factory.authorsDao.create(author), delay) match {
      case Left(e) => fail(e)
      case Right(_) => ()
    }
  }

  private def deleteAuthor(id: ID)(implicit factory: DaoFactory, ec: ExecutionContext): Try[Unit] = Try {
    logger.info(s"deleteAuthor: $id")
    logger.info(s"factory: $factory")
    Await.result(factory.authorsDao.delete(id), delay) match {
      case Left(e) => TearDownException(s"could not delete author $id", Some(e))
      case Right(idOpt) =>
        idOpt match {
          case None => TearDownException(s"did not find author $id to delete")
          case Some(deleted) =>
            logger.info(s"deleted author: $id")
            if (deleted == id) Success()
            else TearDownException(s"deleted wrong author, actual: $deleted, expected: $id")
        }
    }
  }
}

object AuthorDaoImplSpec {

  val jm: Author = Author(
    ID("1"),
    LastName("Milton"),
    FirstName("John"),
    None,
    Some(Address("Bread Street")),
    Some(City("London")),
    Some(State("UK")),
    Some(Zip("12345"))
  )
  val ja: Author = Author(
    ID("2"),
    LastName("Austen"),
    FirstName("Jane"),
    None,
    Some(Address("11 Common Way")),
    Some(City("Steventon")),
    None,
    None
  )
  val cd: Author = Author(
    ID("3"),
    LastName("Dickens"),
    FirstName("Charles"),
    Some(Phone("555-345-6789")),
    Some(Address("Landport")),
    Some(City("Portsmouth")),
    Some(State("UK")),
    None
  )
  val mt: Author = Author(
    ID("4"),
    LastName("Twain"),
    FirstName("Mark"),
    None,
    None,
    Some(City("Hannibal")),
    Some(State("MO")),
    Some(Zip("45678"))
  )
  val eh: Author = Author(
    ID("5"),
    LastName("Hemmingway"),
    FirstName("Ernest"),
    Some(Phone("555-789-0123")),
    None,
    Some(City("Oak Park")),
    Some(State("IL")),
    Some(Zip("60302"))
  )

  val multipleAuthors: Seq[Author] = Seq(ja, jm, cd, mt)
  val authorIds: Seq[ID] = AuthorDaoImplSpec.multipleAuthors.map(_.id)
}