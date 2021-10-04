import kotlin.reflect.*
import kotlin.reflect.jvm.isAccessible

interface Socket<T> {
    val name: String
    val storedClass: KClass<*>
    operator fun getValue(any: Any, property: KProperty<*>): T
    operator fun setValue(any: Any, property: KProperty<*>, t: T)
    fun bindTo(d2: Socket<*>)
}

open class NonNullableSocket<T>(
    override val name: String,
    var storedValue: T,
    override val storedClass: KClass<*>
) : Socket<T> {
    var boundTo: Socket<T>? = null

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

    override fun bindTo(d2: Socket<*>) {
        if (this.storedClass != d2.storedClass) {
            throw TypeCastException()
        }
        @Suppress("UNCHECKED_CAST")
        boundTo = d2 as Socket<T>
    }
}

class LateinitSocket<T>(
    override val name: String,
    override val storedClass: KClass<*>
) : Socket<T> {
    var storedValue: T? = null
    var boundTo: Socket<T>? = null

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

    override fun bindTo(d2: Socket<*>) {
        if (this.storedClass != d2.storedClass) {
            throw TypeCastException()
        }
        @Suppress("UNCHECKED_CAST")
        boundTo = d2 as Socket<T>
    }
}

class NonNullableSocketProvider<T>(
    val storedClass: KClass<*>,
    val sockets: MutableMap<String, Socket<*>>,
    val value: T
) {
    operator fun provideDelegate(any: Any, property: KProperty<*>): NonNullableSocket<T> {
        return NonNullableSocket(property.name, value, storedClass).also {
            sockets.put(property.name, it)
        }
    }
}

class LateinitSocketProvider<T>(
    val storedClass: KClass<*>,
    val sockets: MutableMap<String, Socket<*>>
) {
    operator fun provideDelegate(any: Any, property: KProperty<*>): LateinitSocket<T> {
        return LateinitSocket<T>(property.name, storedClass).also {
            sockets.put(property.name, it)
        }
    }
}

open class Sockets : SocketsHolder {
    val propertiesbyName: MutableMap<String, Socket<*>> = mutableMapOf()

    inline fun <reified T> socket(value: T): NonNullableSocketProvider<T> {
        return NonNullableSocketProvider(T::class, this.propertiesbyName, value)
    }

    inline fun <reified T> lateinitSocket(): LateinitSocketProvider<T> {
        return LateinitSocketProvider(T::class, this.propertiesbyName)
    }

    override fun sockets(): Collection<Socket<*>> {
        return this.propertiesbyName.values
    }

    override operator fun get(socketName: String): Socket<*>? {
        return this.propertiesbyName.get(socketName)
    }

    companion object {
        inline fun <reified V> bind(p1: KMutableProperty0<V>, p2: KProperty0<V>) {
            val d1 = p1.apply { isAccessible = true }.getDelegate()
            val d2 = p2.apply { isAccessible = true }.getDelegate()
            if (d1 is Socket<*> && d2 is Socket<*>) {
                d1.bindTo(d2)
            }
        }
    }

    override val sockets: Sockets = this
}

inline fun <reified V> KMutableProperty0<V>.bindTo(other: KProperty0<V>) {
    Sockets.bind(this, other)
}

interface SocketsHolder {
    val sockets: Sockets
    fun sockets(): Collection<Socket<*>>
    operator fun get(socketName: String): Socket<*>?
}

inline fun <reified T> SocketsHolder.socket(value: T) = this.sockets.socket(value)

inline fun <reified T> SocketsHolder.lateinitSocket() = this.sockets.lateinitSocket<T>()
