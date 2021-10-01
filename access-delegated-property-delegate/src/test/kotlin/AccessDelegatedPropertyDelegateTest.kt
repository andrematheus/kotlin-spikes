import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.*
import kotlin.reflect.jvm.isAccessible

class AccessDelegatedPropertyDelegateTest : ExpectSpec({
    context("A class with a delegated property") {
        class Delegate {
            var value: String = "initial"

            operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
                return "delegated property with: $value"
            }
        }

        class TestClass {
            val property: String by Delegate()
        }

        expect("delegated property can be accessed") {
            val testObject = TestClass()
            testObject.property shouldBe "delegated property with: initial"
        }

        expect("delegate object may be accessed through reflection") {
            val testObject = TestClass()
            val delegate = testObject::property.apply { isAccessible = true }.getDelegate()
            delegate!!::class shouldBe Delegate::class

            expect("delegate object may be used to change the property") {
                @Suppress("NAME_SHADOWING") val delegate = delegate as Delegate
                delegate.value = "new value"
                testObject.property shouldBe "delegated property with: new value"
            }
        }
    }
})