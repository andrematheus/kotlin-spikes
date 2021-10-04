import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class SocketsSpec : FeatureSpec({
    feature("Sockets") {
        open class CommonClassWithSockets(
            sockets: Sockets = Sockets()
        ) : SocketsHolder by sockets

        class AClassWithSockets : CommonClassWithSockets() {
            var lateinitSocket by lateinitSocket<String>()
            var socketWithDefaultValue by socket("Default value")
            var nullableSocket by socket<String?>(null)
            var socket by socket("")
        }

        class AnotherClassWithSockets : CommonClassWithSockets() {
            var socket by socket("")
            var otherSocket by socket(1)
        }

        val subject = AClassWithSockets()
        val otherSubject = AnotherClassWithSockets()

        scenario("lateinit socket defaults to uninitialized") {
            shouldThrow<UninitializedPropertyAccessException> { subject.lateinitSocket }
        }

        scenario("lateinit socket may be changed") {
            subject.lateinitSocket = "Some string"
            subject.lateinitSocket shouldBe "Some string"
        }

        scenario("socket may have a default value") {
            subject.socketWithDefaultValue shouldBe "Default value"
        }

        scenario("socket may be nullable") {
            subject.nullableSocket shouldBe null
        }

        scenario("socket may be non-nullable keeping type") {
            subject.nullableSocket = null
        }

        scenario("two sockets of same type my be bound") {
            subject::socket.bindTo(otherSubject::socket)
            otherSubject.socket = "Changed value"
            subject.socket shouldBe otherSubject.socket
        }

        scenario("sockets should be listable by name") {
            subject.sockets().map { it.name } shouldBe listOf(
                "lateinitSocket",
                "socketWithDefaultValue",
                "nullableSocket",
                "socket"
            )
        }

        scenario("sockets should be bindable by name") {
            subject["property"]?.bindTo(otherSubject["property"]!!)
            otherSubject.socket = "Changed value"
            subject.socket shouldBe otherSubject.socket
        }

        scenario("socket binding should check types") {
            shouldThrow<TypeCastException> {
                subject["socket"]!!.bindTo(otherSubject["otherSocket"]!!)
            }
        }

        scenario("socket name should be inspectable") {
            val socket = subject["socket"]!!
            socket.name shouldBe "socket"
        }

        scenario("socket type should be inspectable") {
            val socket = subject["socket"]!!
            socket.storedClass shouldBe String::class
        }
    }
})
