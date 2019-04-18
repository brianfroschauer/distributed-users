package repositories

import com.google.protobuf.timestamp.Timestamp
import database.{Db, UserTable}
import models.User
import slick.basic.DatabaseConfig
import slick.jdbc.H2Profile
import slick.lifted

import scala.concurrent.Future

class UserRepository (val config: DatabaseConfig[H2Profile])
  extends Db with UserTable {

  import config.profile.api._

  private val users = lifted.TableQuery[UserTable]

  db.run(DBIO.seq(users.schema.create))

  def create(firstName: String, lastName: String, email: String): Future[User] = db.run (
    (users.map(user => (user.firstName, user.lastName, user.email, user.timestamp))
      returning users.map(_.userId)
      into ((userData, userId) => User(userId, userData._1, userData._2, userData._3, userData._4))
      ) += (firstName, lastName, email, Timestamp.toString)
  )

  def update(user: User): Future[Int] = {
    db.run(users.update(user))
  }

  def list(): Future[Seq[User]] = db.run (
    users.result
  )

  def getById(userId: Long): Future[Option[User]] = db.run (
    users.filter{_.userId === userId}.result.headOption
  )
}
