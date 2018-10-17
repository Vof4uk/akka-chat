import java.time.ZonedDateTime

case class Message(sender: User, body: String, sent: ZonedDateTime)
