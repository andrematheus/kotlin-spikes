import kotlin.reflect.*
import kotlin.reflect.jvm.isAccessible

interface BindableProperty<T> {
    val name: String
    val storedClass: KClass<*>
    operator fun getValue(any: Any, property: KProperty<*>): T
    operator fun setValue(any: Any, property: KProperty<*>, t: T)
    fun bindTo(d2: BindableProperty<*>)
}

open class NonNullableBindableProperty<T>(
    override val name: String, var storedValue: T,
    override val storedClass: KClass<*>
) : BindableProperty<T> {
    var boundTo: BindableProperty<T>? = null

    override operator fun getValue(any: Any, property: KProperty<*>): T {
        val boundTo = this.boundTo
        return if (boundTo != null) {
            boundTo.getValue(any, property)
        } else {
            this.storedValue
        }
    }

    override operator fun setValue(any: Any, property: KProperty<*>, t: T) {
        val boundTo = this.boundTo
        if (boundTo != null) {
            boundTo.setValue(any, property, t)
        } else {
            storedValue = t
        }
    }

    override fun bindTo(d2: BindableProperty<*>) {
        if (this.storedClass != d2.storedClass) {
            throw TypeCastException()
        }
        boundTo = d2 as BindableProperty<T>
    }
}

class LateBindableProperty<T>(override val name: String, override val storedClass: KClass<*>) : BindableProperty<T> {
    var storedValue: T? = null
    var boundTo: BindableProperty<T>? = null

    override operator fun getValue(any: Any, property: KProperty<*>): T {
        val boundTo = this.boundTo
        return if (boundTo != null) {
            boundTo.getValue(any, property)
        } else {
            storedValue ?: throw UninitializedPropertyAccessException()
        }
    }

    override operator fun setValue(any: Any, property: KProperty<*>, t: T) {
        val boundTo = this.boundTo
        if (boundTo != null) {
            boundTo.setValue(any, property, t)
        } else {
            storedValue = t
        }
    }

    override fun bindTo(d2: BindableProperty<*>) {
        if (this.storedClass != d2.storedClass) {
            throw TypeCastException()
        }
        boundTo = d2 as BindableProperty<T>
    }
}

class NonNullableBindablePropertyProvider<T>(
    val storedClass: KClass<*>,
    val props: MutableMap<String, BindableProperty<*>>,
    val value: T
) {
    operator fun provideDelegate(any: Any, property: KProperty<*>): NonNullableBindableProperty<T> {
        return NonNullableBindableProperty(property.name, value, storedClass).also {
            props.put(property.name, it)
        }
    }
}

class LateBindablePropertyProvider<T>(
    val storedClass: KClass<*>,
    val properties: MutableMap<String, BindableProperty<*>>
) {
    operator fun provideDelegate(any: Any, property: KProperty<*>): LateBindableProperty<T> {
        return LateBindableProperty<T>(property.name, storedClass).also {
            properties.put(property.name, it)
        }
    }
}

open class Props : PropsHolder {
    val propertiesbyName: MutableMap<String, BindableProperty<*>> = mutableMapOf()

    inline fun <reified T> prop(value: T): NonNullableBindablePropertyProvider<T> {
        return NonNullableBindablePropertyProvider(T::class, this.propertiesbyName, value)
    }

    inline fun <reified T> lateinitProp(): LateBindablePropertyProvider<T> {
        return LateBindablePropertyProvider(T::class, this.propertiesbyName)
    }

    fun properties(): Collection<BindableProperty<*>> {
        return this.propertiesbyName.values
    }

    operator fun get(propertyName: String): BindableProperty<*>? {
        return this.propertiesbyName.get(propertyName)
    }

    companion object {
        inline fun <reified V> bind(p1: KMutableProperty0<V>, p2: KProperty0<V>) {
            val d1 = p1.apply { isAccessible = true }.getDelegate()
            val d2 = p2.apply { isAccessible = true }.getDelegate()
            if (d1 is BindableProperty<*> && d2 is BindableProperty<*>) {
                d1.bindTo(d2)
            }
        }
    }

    override val properties: Props = this
}

interface PropsHolder {
    val properties: Props
}

inline fun <reified T> PropsHolder.prop(value: T) = this.properties.prop(value)

inline fun <reified T> PropsHolder.lateinitProp() = this.properties.lateinitProp<T>()
