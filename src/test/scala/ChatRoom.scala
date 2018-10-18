import java.time.ZonedDateTime

import ChatRoom.SimpleMessage
import akka.actor.{ActorSystem, Props}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestActors, TestKit}
import com.typesafe.config.ConfigFactory
import model.{Message, User}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class ChatUserActorSpec
  extends TestKit(ActorSystem(
    "ChatUserActorSpec",
    ConfigFactory.parseString(ChatUserActorSpec.config)))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    shutdown()
  }


  val testUser = User("id001", "testNick")
  val userRef = system.actorOf(Props(classOf[ChatUser], testUser))
  val testedUserRef = TestActorRef(new ChatUser(testUser))

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

object ChatUserActorSpec {
  // Define your test specific configuration here
  val config =
    """
    akka {
      loglevel = "WARNING"
    }
    """
}
