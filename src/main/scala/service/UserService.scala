package service

import io.grpc.{ManagedChannel, ManagedChannelBuilder, Status, StatusRuntimeException}
import models.User
import repositories.UserRepository
import server.ServiceManager
import user.user._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class UserService(userRepository: UserRepository,
                  serviceManager: ServiceManager)
                 (implicit ec: ExecutionContext)
  extends UserServiceGrpc.UserService {

  override def addUser(request: AddUserRequest): Future[AddUserResponse] = {
    userRepository.create(request.firstName, request.lastName) map {
      user => AddUserResponse(user.userId)
    }
  }

  override def getUser(request: GetUserRequest): Future[GetUserResponse] = {
    userRepository.getById(request.userId).map(optionalUser => {
      val user: User = optionalUser.get
      GetUserResponse(user.userId, user.firstName, user.lastName)
    })
  }

  override def getUsers(request: GetUsersRequest): Future[GetUsersResponse] = {
    val eventualUsers = userRepository.list()

    val result = getUserServiceStub.flatMap(stub => {
      eventualUsers.map(users => users.map(
        user => stub.getUser(GetUserRequest(user.userId))
      )).flatMap(response => Future.sequence(response))
    })

    val value = Await.ready(result, Duration.apply(5, "second")).value.get

    value match {
      case Success(users) => Future.successful(GetUsersResponse(users))
      case Failure(exception: StatusRuntimeException) =>
        if (exception.getStatus.getCode == Status.Code.UNAVAILABLE) {
          getUsers(request)
        } else throw exception
    }

  }

  override def updateUser(request: UpdateUserRequest): Future[UpdateUserResponse] = {
    userRepository.update(User(request.userId, request.firstName, request.lastName))
      .map(userId => UpdateUserResponse(userId))
  }

  override def deleteUser(request: DeleteUserRequest): Future[DeleteUserResponse] = ???

  override def isActive(request: PingRequest): Future[PingResponse] = ???

  private def getUserServiceStub: Future[UserServiceGrpc.UserServiceStub] = {
    serviceManager.getAddress("users").map {
        case Some(value) =>
          val channel: ManagedChannel = ManagedChannelBuilder.forAddress(value.address, value.port).build()
          UserServiceGrpc.stub(channel)
        case None => throw new RuntimeException("No users services running")
      }
  }
}

case object UserNotFoundException extends RuntimeException
