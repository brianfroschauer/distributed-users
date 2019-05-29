package server

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import io.grpc.ServerBuilder
import proto.user.UserServiceGrpc
import repositories.UserRepository
import service.UserService
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

object UserServer extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  // root is the default value for user and password
  val user: String = args.lift(0).getOrElse("root")
  val password: String = args.lift(1).getOrElse("root")

  /*val serviceManager = new ServiceManager
  serviceManager.startConnection("localhost", 50003, "user")*/

  val config: Config = ConfigFactory.load("db")
  val url: String = s"jdbc:mysql://localhost:3306/test?user=$user&password=$password"
  val newConfig = config.withValue("db.db.url", ConfigValueFactory.fromAnyRef(url))

  val databaseConfig = DatabaseConfig.forConfig[MySQLProfile]("db", newConfig)

  val userRepository = new UserRepository(databaseConfig)

  val server = ServerBuilder.forPort(50001)
    .addService(UserServiceGrpc.bindService(new UserService(userRepository), ExecutionContext.global))
    .build()

  server.start()

  println("Running...")

  server.awaitTermination()
}

/*object ClientDemo extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val serviceManager = new ServiceManager
  val address = Await.ready(serviceManager.getAddress("user"), Duration(5, "second")).value.get.get

  val channel = ManagedChannelBuilder.forAddress(address.get.address, address.get.port)
    .usePlaintext(true)
    .build()

  val stub = UserServiceGrpc.stub(channel)

  println("\nAdding user...")

  val user = stub.addUser(AddUserRequest("Brian", "Froschauer", "mail@example.com"))

  print("User added successfully")

  user.onComplete { response =>
    println("\nUser id in the response: " + response.get.userId)
  }

  System.in.read()

}*/
