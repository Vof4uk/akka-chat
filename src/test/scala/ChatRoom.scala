import java.time.ZonedDateTime

import ChatRoom.{SimpleMessage, UserJoined, UserLeft}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import model.{Message, User}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration._

class ChatUserActorSpec
  extends TestKit(ActorSystem(
    "ChatUserActorSpec",
    ConfigFactory.parseString(ChatRoomSpec.config)))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  override def afterAll: Unit = {
    shutdown()
  }


  override protected def beforeEach(): Unit = {
    testedUserRef = TestActorRef(new ChatUser(testUser))
  }

  var testedUserRef = TestActorRef(new ChatUser(testUser))
  val testUser = User("id001", "testNick")
  val userRef = system.actorOf(Props(classOf[ChatUser], testUser))

  "A ChatUser actor" should {
    "Respond nothing to Message instances" in {
      within(500 millis) {
        userRef ! SimpleMessage(Message(testUser, "hi", ZonedDateTime.now))
        expectNoMessage()
      }
    }
  }

  "A ChatUser actor" should {
    "Fail with UnsupportedOperationException" in {

      intercept[UnsupportedOperationException] {
        testedUserRef.receive("Hi")
      }
    }
  }
}


class ChatRoomActorSpec
  extends TestKit(ActorSystem(
    "ChatUserActorSpec",
    ConfigFactory.parseString(ChatRoomSpec.config)))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  override def afterAll: Unit = {
    shutdown()
  }


  override protected def beforeEach(): Unit = {
    testedRoomRef = TestActorRef(new ChatRoom)
  }
  var testedRoomRef = TestActorRef(new ChatRoom)

  val testUser = User("id001", "testNick")
  val chatRoomRef = system.actorOf(Props(classOf[ChatRoom]))

  "A ChatRoom actor" should {
    "Respond nothing to Message instances when valid message" in {
      within(500 millis) {
        chatRoomRef ! UserJoined(testUser)
        chatRoomRef ! SimpleMessage(Message(testUser, "hi", ZonedDateTime.now))
        expectNoMessage()
      }
    }
  }

  "A ChatRoom actor" should {
    "Respond nothing to UserJoin event" in {
      within(500 millis) {
        chatRoomRef ! UserJoined(testUser)
        expectNoMessage()
      }
    }
  }

  "A ChatRoom actor" should {
    "Respond nothing to UserLeft event" in {
      within(500 millis) {
        chatRoomRef ! UserJoined(testUser)
        chatRoomRef ! UserLeft(testUser)
        expectNoMessage()
      }
    }
  }

  "A ChatUser actor" should {
    "Fail with IllegalStateException when join user already present" in {
      intercept[IllegalStateException] {
        testedRoomRef.receive(UserJoined(testUser))
        testedRoomRef.receive(UserJoined(testUser))
      }
    }
  }

  "A ChatUser actor" should {
    "Fail with IllegalStateException when user leaving being not in chat" in {
      intercept[IllegalStateException] {
        testedRoomRef.receive(UserLeft(testUser))
      }
    }
  }
}

object ChatRoomSpec {
  // Define your test specific configuration here
  val config =
    """
    akka {
      loglevel = "WARNING"
    }
    """
}
