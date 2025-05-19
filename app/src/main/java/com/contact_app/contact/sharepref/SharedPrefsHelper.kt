package com.contact_app.contact.sharepref

interface SharedPrefsHelper {
    fun <T> put(keyName: String, value: T)

    fun <T> putList(keyName: String, value: List<T>)

    fun <T> get(keyName: String, clazz: Class<T>, default: T? = null): T?

    fun <T> getList(keyName: String, clazz: Class<T>): List<T>?

    fun clear()

    fun remove(keyName: String)
}