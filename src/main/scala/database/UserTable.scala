package database

import models.User

trait UserTable {
  this: Db =>

  import config.profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, "user") {

    def userId = column[Long]("user_id", O.PrimaryKey, O.AutoInc)
    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def * = (userId, firstName, lastName) <> ((User.apply _).tupled, User.unapply)
  }
}
