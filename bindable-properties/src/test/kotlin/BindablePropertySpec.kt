import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class BindablePropertySpec : FeatureSpec({
    feature("Bindable Property") {
        open class ClassWithProps(
            props: Props = Props()
        ) : PropsHolder by props

        class ClassWithBindableProps : ClassWithProps() {
            var lateBindable by lateinitProp<String>()
            val propertyWithDefaultValue by prop("Default value")
            var nullableProperty by prop<String?>(null)
            var property by prop("")
        }

        class AnotherClassWithBindableProps : ClassWithProps() {
            var property by prop("")
            var otherProperty by prop(1)
        }

        val subject = ClassWithBindableProps()
        val otherSubject = AnotherClassWithBindableProps()

        scenario("late bindable property defaults to uninitialized") {
            shouldThrow<UninitializedPropertyAccessException> { subject.lateBindable }
        }

        scenario("late bindable property may be changed") {
            subject.lateBindable = "Some string"
            subject.lateBindable shouldBe "Some string"
        }

        scenario("property may have a default value") {
            subject.propertyWithDefaultValue shouldBe "Default value"
        }

        scenario("property may be nullable") {
            subject.nullableProperty shouldBe null
        }

        scenario("property may be non-nullable keeping type") {
            subject.nullableProperty = null
        }

        scenario("two properties of same type my be bound") {
            Props.bind(subject::property, otherSubject::property)
            otherSubject.property = "Changed value"
            subject.property shouldBe otherSubject.property
        }

        scenario("properties should be listable by name") {
            subject.properties.properties().map { it.name } shouldBe listOf(
                "lateBindable",
                "propertyWithDefaultValue",
                "nullableProperty",
                "property"
            )
        }

        scenario("properties should be bindable by name") {
            subject.properties["property"]?.bindTo(otherSubject.properties["property"]!!)
            otherSubject.property = "Changed value"
            subject.property shouldBe otherSubject.property
        }

        scenario("property binding should check types") {
            shouldThrow<TypeCastException> {
                subject.properties["property"]?.bindTo(otherSubject.properties["otherProperty"]!!)
            }
        }

        scenario("property type should be inspectable") {
            val prop = subject.properties["property"]!!
            prop.storedClass shouldBe String::class
        }
    }
})
