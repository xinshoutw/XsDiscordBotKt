package core.command

import org.slf4j.LoggerFactory

class ComponentRegistry {
    private val logger = LoggerFactory.getLogger(ComponentRegistry::class.java)
    private val handlers = mutableMapOf<String, ComponentHandler>()

    fun register(handler: ComponentHandler) {
        handlers[handler.prefix] = handler
        logger.debug("Registered component handler with prefix: {}", handler.prefix)
    }

    fun deregisterByPrefix(prefix: String) {
        handlers.remove(prefix)
        logger.debug("Deregistered component handler with prefix: {}", prefix)
    }

    fun findHandler(componentId: String): ComponentHandler? {
        return handlers.entries.find { componentId.startsWith(it.key) }?.value
    }

    fun clear() {
        handlers.clear()
    }
}
