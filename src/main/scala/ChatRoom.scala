import java.time.ZonedDateTime

import ChatRoom.{SimpleMessage, UserJoined, UserLeft}
import akka.actor.{Actor, ActorRef, Props}
import model.{Message, User}

/**
  * Companion object for ChatRoom actor
  */
object ChatRoom {

  sealed trait ChatEvent

  /**
    * Plain text message
    * @param message body
    */
  case class SimpleMessage(message: Message) extends ChatEvent

  /**
    * Event reflects that user joined the chat
    * @param user new user
    */
  case class UserJoined(user: User)

  /**
    * Event reflects that user left the chat
    * @param user leaving user
    */
  case class UserLeft(user: User)

  /**
    * Props to create the Actor
    */
  val props = Props[ChatRoom]

}

/**
  * Actor reflects chat Room abstraction from message delivery perspective.
  * List of chat members subscribed to receive messages inside this room.
  */
class ChatRoom extends Actor {
  //todo replace with some repo
  private var chatUsers: Map[User, ActorRef] = Map()

  private val ChatroomMaster = User("NoId", "admin bot")

  /**
    * Deliver message to all chat members
    * @param message to deliver
    */
  def broadcastMessage(message: Message): Unit = chatUsers.filter(_._1 != message.sender).foreach(_._2 ! SimpleMessage(message))

  def receive = {
    case SimpleMessage(msg) =>
      userMustPresent(msg.sender)
      broadcastMessage(msg)
    case UserJoined(user) =>
      userMustAbsent(user)
      broadcastMessage(Message(ChatroomMaster, s"User ${user.nickName} just joined", ZonedDateTime.now()))
      chatUsers = chatUsers + (user -> context.actorOf(ChatUser.props(user), user.nickName))
      chatUsers(user) ! SimpleMessage(Message(ChatroomMaster, s"Welcome to our chat, ${user.nickName}!", ZonedDateTime.now()))
    case UserLeft(user) =>
      userMustPresent(user)
      context.stop(chatUsers(user))
      chatUsers = chatUsers.filter(_._1 != user)
      broadcastMessage(Message(ChatroomMaster, s"User ${user.nickName} just left", ZonedDateTime.now()))
  }

  /**
    * Validate that user is present in the chat otherwise throw IllegalStateException.
    * @param user lookup instance
    */
  private def userMustPresent(user: User): Unit =
    if (!chatUsers.contains(user))
      throw new IllegalStateException(s"No such user in chat ${user}")
    else
      ()

  /**
    * Validate that user is not present in the chat otherwise throw IllegalStateException.
    * @param user lookup instance
    */
  private def userMustAbsent(user: User): Unit =
    if (chatUsers.contains(user))
      throw new IllegalStateException(s"User ${user} already present.")
    else
      ()
}

/**
  * Companion object for ChatUser.class
  */
object ChatUser {

  /**
    * Props to create an actor instance.
    * @param user that reflects the joining chat member
    * @return props
    */
  def props(user: User) = Props(new ChatUser(user))
}

/**
  * This actor reflects abstraction of chat member from message delivery perspective.
  * //todo Currently ChatUser actor just prints out text messages to console.
  * //todo In the future messages will be passed to chat-client.
  * @param user User instance with with all info to define the chat client.
  */
class ChatUser(val user: User) extends Actor {
  def receive = {
    case SimpleMessage(msg) =>
      println(s"[${msg.sender}]:[${msg.sent}] - ${msg.body}")
    case unknown => throw new UnsupportedOperationException(s"Unknown command ${unknown.getClass.getSimpleName}.")
  }
}


