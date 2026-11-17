package onboarding.bff.api

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Runs [block] on the IO dispatcher keeping the MDC and the context class loader. */
suspend fun <T> withCommonContext(block: suspend () -> T): T =
    withContext(IO + MDCContext() + ClassLoaderContextElement()) { block() }

/** Needed because of https://github.com/quarkusio/quarkus/issues/40245 */
class ClassLoaderContextElement : ThreadContextElement<ClassLoader> {
    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader

    companion object Key : CoroutineContext.Key<ClassLoaderContextElement>

    override val key: CoroutineContext.Key<ClassLoaderContextElement>
        get() = Key

    override fun updateThreadContext(context: CoroutineContext): ClassLoader {
        val oldClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = classLoader
        return oldClassLoader
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: ClassLoader,
    ) {
        Thread.currentThread().contextClassLoader = oldState
    }
}
