import java.time.ZonedDateTime

import ChatRoom.{SimpleMessage, UserJoined, UserLeft}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import model.{Message, User}

object ChatRoom {

  sealed trait ChatEvent

  case class SimpleMessage(message: Message) extends ChatEvent

  case class UserJoined(user: User)

  case class UserLeft(user: User)

  val props = Props[ChatRoom]

}

class ChatRoom extends Actor {
  private var chatUsers: Map[User, ActorRef] = Map()
  //todo replace with some repo
  private val ChatroomMaster = User("NoId", "admin bot")

  def deliverMessage(message: Message): Unit = chatUsers.filter(_._1 != message.sender).foreach(_._2 ! SimpleMessage(message))

  def receive = {
    case SimpleMessage(msg) =>
      userMustPresent(msg.sender)
      deliverMessage(msg)
    case UserJoined(user) =>
      userMustAbsent(user)
      deliverMessage(Message(ChatroomMaster, s"User ${user.nickName} just joined", ZonedDateTime.now()))
      chatUsers = chatUsers + (user -> context.actorOf(ChatUser.props(user), user.nickName))
      chatUsers(user) ! SimpleMessage(Message(ChatroomMaster, s"Welcome to our chat, ${user.nickName}!", ZonedDateTime.now()))
    case UserLeft(user) =>
      userMustPresent(user)
      context.stop(chatUsers(user))
      chatUsers = chatUsers.filter(_._1 != user)
      deliverMessage(Message(ChatroomMaster, s"User ${user.nickName} just left", ZonedDateTime.now()))
  }

  private def userMustPresent(user: User): Unit =
    if (!chatUsers.contains(user))
      throw new IllegalStateException(s"No such user in chat ${user}")
    else
      ()

  private def userMustAbsent(user: User): Unit =
    if (chatUsers.contains(user))
      throw new IllegalStateException(s"User ${user} already present.")
    else
      ()
}

object ChatUser {

  sealed trait ChatCommand

  def props(user: User) = Props(new ChatUser(user))
}

class ChatUser(val user: User) extends Actor {
  def receive = {
    case SimpleMessage(msg) =>
      println(s"[${msg.sender}]:[${msg.sent}] - ${msg.body}")
    case unknown => throw new UnsupportedOperationException(s"Unknown command $unknown.")
  }
}

object Chat extends App {
  val lilly69 = User("001111", "lilly69")
  val kolya84 = User("001112", "kolia84")

  val aSys = ActorSystem("test-chat")
  val chatRoom = aSys.actorOf(ChatRoom.props)
  chatRoom ! UserJoined(lilly69)
  Thread.sleep(10L)
  chatRoom ! UserJoined(kolya84)
  Thread.sleep(10L)
  //  chatRoom ! UserLeft(kolya84)
  chatRoom ! UserLeft(kolya84)
  Thread.sleep(10L)
  chatRoom ! UserJoined(kolya84)
  Thread.sleep(10L)
  chatRoom ! SimpleMessage(Message(lilly69, "Hi", ZonedDateTime.now()))
  Thread.sleep(10L)
  aSys.terminate()
}
