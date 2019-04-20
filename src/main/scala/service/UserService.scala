package service

import com.google.protobuf.timestamp.Timestamp
import models.User
import proto.user._
import repositories.UserRepository
import server.ServiceManager
import scala.concurrent.{ExecutionContext, Future}

class UserService(userRepository: UserRepository,
                  serviceManager: ServiceManager)
                 (implicit ec: ExecutionContext)
  extends UserServiceGrpc.UserService {


  override def addUser(request: AddUserRequest): Future[AddUserResponse] = {
    userRepository.create(request.firstName, request.lastName, request.email) map {
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

    val eventualResponses = eventualUsers.map(users =>
      users.map(user =>
        GetUserResponse(user.userId, user.firstName, user.lastName, user.email)))

    eventualResponses.map(res => GetUsersResponse(res))
  }

  override def updateUser(request: UpdateUserRequest): Future[UpdateUserResponse] = {
    userRepository.update(User(request.userId, request.firstName, request.lastName, request.email, Timestamp.SECONDS_FIELD_NUMBER))
      .map(userId => UpdateUserResponse(userId))
  }

  override def deleteUser(request: DeleteUserRequest): Future[DeleteUserResponse] = {
    userRepository.delete(request.userId)
    Future.successful(DeleteUserResponse())
  }

  override def isActive(request: PingRequest): Future[PingResponse] = ???
}

case object UserNotFoundException extends RuntimeException
