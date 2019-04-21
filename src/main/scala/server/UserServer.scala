package server

import io.grpc.{ManagedChannelBuilder, ServerBuilder}
import proto.user.{AddUserRequest, UserServiceGrpc}
import repositories.UserRepository
import service.UserService
import slick.basic.DatabaseConfig
import slick.jdbc.H2Profile

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

object UserServer extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val serviceManager = new ServiceManager
  serviceManager.startConnection("localhost", 50003, "user")

  val config = DatabaseConfig.forConfig[H2Profile]("db")
  val userRepository = new UserRepository(config)

  val server = ServerBuilder.forPort(50003)
    .addService(UserServiceGrpc.bindService(
      new UserService(userRepository), ExecutionContext.global))
    .build()

  server.start()

  println("Running...")

  server.awaitTermination()
}

object ClientDemo extends App {

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

}
