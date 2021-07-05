package net.sourcebot.api.module

abstract class ModuleClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    final override fun findClass(name: String): Class<*> = findClass(name, true)
    abstract fun findClass(name: String, searchParent: Boolean): Class<*>
}