package server

import io.grpc.{ManagedChannelBuilder, ServerBuilder}
import proto.user.{AddUserRequest, UserServiceGrpc}
import repositories.UserRepository
import service.UserService
import slick.basic.DatabaseConfig
import slick.jdbc.H2Profile

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

object UserServer extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val stubManager = new ServiceManager

  stubManager.startConnection("0.0.0.0", 50001, "users")

  val config = DatabaseConfig.forConfig[H2Profile]("db")
  val userRepository = new UserRepository(config)

  val server = ServerBuilder.forPort(50001)
    .addService(UserServiceGrpc.bindService(new UserService(userRepository, stubManager), ExecutionContext.global))
    .build()

  server.start()
  println("Running...")

  server.awaitTermination()
}

object ClientDemo extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val channel = ManagedChannelBuilder.forAddress("localhost", 50001).build()

  val stub = UserServiceGrpc.stub(channel)

  val user = stub.addUser(AddUserRequest("Brian", "Froschauer"))
  user.onComplete { response =>
    println(response.get.userId)
  }

  val stubManager = new ServiceManager()

  stubManager.getAddress("users").onComplete {
    case Success(value) => println(value.get.port)
    case Failure(exception) => println(exception)
  }

  System.in.read()
}
