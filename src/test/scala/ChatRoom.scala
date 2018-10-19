import java.time.ZonedDateTime

import ChatRoom.{SimpleMessage, UserJoined, UserLeft}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.TestActors.ForwardActor
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit, TestProbe}
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
//    forwardReceiver = TestProbe()
    chatRoomRef = system.actorOf(Props(classOf[ChatRoom]))
  }


  override protected def afterEach(): Unit = {
//    system.stop(testedRoomRef).wait()
//    system.stop(chatRoomRef).wait()
  }

  var testedRoomRef = TestActorRef(new ChatRoom)

  val testUser1 = User("id001", "Alfred")
  val testUser2 = User("id002", "Thomas")


  var forwardReceiver = TestProbe()
  var chatRoomRef = system.actorOf(Props(classOf[ChatRoom]))
  val dummyActor = TestActorRef(new Actor{
    def receive = { case x => x }
  })

  "A ChatRoom actor" should {
    "Respond nothing to Message instances when valid message" in {
      within(500 millis) {
        chatRoomRef ! UserJoined(testUser1, TestActorRef(forwardReceiver.ref))
        chatRoomRef ! SimpleMessage(Message(testUser1, "hi", ZonedDateTime.now))
        expectNoMessage()
      }
    }
  }

  "A ChatRoom actor" should {
    "not pass messages to user who sent them" in {
      within(500 millis) {
        chatRoomRef ! UserJoined(testUser1, TestActorRef(new ForwardActor(forwardReceiver.ref)))
        chatRoomRef ! SimpleMessage(Message(testUser1, "hi", ZonedDateTime.now))
        forwardReceiver.expectMsgClass(classOf[SimpleMessage])
        forwardReceiver.expectNoMessage(300 millis)
      }
    }
  }

  "A ChatRoom actor" should {
    "not pass messages to user who entered the room" in {
      within(500 millis) {
        chatRoomRef ! UserJoined(testUser1, TestActorRef(new ForwardActor(forwardReceiver.ref)))
        forwardReceiver.expectMsgClass(classOf[SimpleMessage])
        forwardReceiver.expectNoMessage(300 millis)
      }
    }
  }

  "A ChatRoom actor" should {
    "notify other users when new user joined" in {
      within(500 millis) {
        chatRoomRef ! UserJoined(testUser1, TestActorRef(new ForwardActor(forwardReceiver.ref)))
        chatRoomRef ! UserJoined(testUser2, dummyActor)
        forwardReceiver.expectMsgClass(classOf[SimpleMessage])
      }
    }
  }
  "A ChatUser actor" should {
    "Fail with IllegalStateException when join user already present" in {
      intercept[IllegalStateException] {
        testedRoomRef.receive(UserJoined(testUser1, dummyActor))
        testedRoomRef.receive(UserJoined(testUser1, dummyActor))
      }
    }
  }

  "A ChatUser actor" should {
    "Fail with IllegalStateException when user leaving being not in chat" in {
      intercept[IllegalStateException] {
        testedRoomRef.receive(UserLeft(testUser1))
      }
    }
  }

  "A ChatRoom actor" should {
    "deliver messages to all but sender" in {
      within(500 millis) {
        chatRoomRef ! UserJoined(testUser1, TestActorRef(new ForwardActor(forwardReceiver.ref)))
        chatRoomRef ! UserJoined(testUser2, dummyActor)
        chatRoomRef ! SimpleMessage(Message(testUser2, "hi", ZonedDateTime.now))
        forwardReceiver.expectMsgClass(classOf[SimpleMessage])
      }
    }
  }

  "A ChatUser actor if present" should {
    "Fail with IllegalStateException when join again" in {
      intercept[IllegalStateException] {
        testedRoomRef.receive(UserJoined(testUser1, dummyActor))
        testedRoomRef.receive(UserJoined(testUser1, dummyActor))
      }
    }
  }

//  "A ChatRoom actor" should {
//    "Fail with IllegalStateException when join user already present" in {
//      within(500 millis) {
//        val actor = TestActorRef(new ForwardActor(forwardReceiver.ref))
//        chatRoomRef ! UserJoined(testUser1, actor)
//        chatRoomRef ! UserJoined(testUser1, actor)
//        forwardReceiver.expectMsgClass(classOf[SimpleMessage])
//        forwardReceiver.(classOf[SimpleMessage])
//      }
//    }
//  }

//  "A ChatRoom actor" should {
//    "Respond nothing to UserJoin event" in {
//      within(500 millis) {
//        chatRoomRef ! UserJoined(testUser1)
//        expectNoMessage()
//      }
//    }
//  }
//
//  "A ChatRoom actor" should {
//    "Respond nothing to UserLeft event" in {
//      within(500 millis) {
//        chatRoomRef ! UserJoined(testUser1)
//        chatRoomRef ! UserLeft(testUser1)
//        expectNoMessage()
//      }
//    }
//  }
//

//
//  "A ChatUser actor" should {
//    "Fail with IllegalStateException when user leaving being not in chat" in {
//      intercept[IllegalStateException] {
//        testedRoomRef.receive(UserLeft(testUser1))
//      }
//    }
//  }
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
