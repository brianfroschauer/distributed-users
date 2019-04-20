package server

import io.grpc.{ManagedChannelBuilder, ServerBuilder}
import proto.user.{AddUserRequest, UserServiceGrpc}
import repositories.UserRepository
import service.UserService
import slick.basic.DatabaseConfig
import slick.jdbc.H2Profile

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object UserServer extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val config = DatabaseConfig.forConfig[H2Profile]("db")
  val userRepository = new UserRepository(config)

  val channel = ManagedChannelBuilder.forAddress("localhost", 50000)
    .usePlaintext(true)
    .build()

  val stub = UserServiceGrpc.stub(channel)

  val server = ServerBuilder.forPort(50000)
    .addService(UserServiceGrpc.bindService(new UserService(userRepository), ExecutionContext.global))
    .build()

  server.start()

  println("Running...")

  server.awaitTermination()
}

object ClientDemo extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val config = DatabaseConfig.forConfig[H2Profile]("db")
  val userRepository = new UserRepository(config)

  val channel = ManagedChannelBuilder.forAddress("localhost", 50000)
    .usePlaintext(true)
    .build()

  val stub = UserServiceGrpc.stub(channel)

  val server = ServerBuilder.forPort(50000)
    .addService(UserServiceGrpc.bindService(new UserService(userRepository), ExecutionContext.global))
    .build()

  server.start()

  println("Running...")

  println("\nAdding user...")

  val user = stub.addUser(AddUserRequest("Brian", "Froschauer", "mail@example.com"))

  print("User added successfully")

  user.onComplete { response =>
    println("\nUser id in the response: " + response.get.userId)
  }

  System.in.read()

  server.awaitTermination()
}
