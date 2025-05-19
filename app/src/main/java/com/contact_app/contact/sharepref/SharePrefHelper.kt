package com.contact_app.contact.sharepref

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharePrefHelper(
    private val context: Context,
    private val gson: Gson
) : SharedPrefsHelper {

    private val sharedPreferences by lazy {
        context.getSharedPreferences(KEY_SHARE_PREF, Context.MODE_PRIVATE)
    }

    override fun <T> put(keyName: String, value: T) {
        val editor = sharedPreferences.edit()
        when (value) {
            is String -> editor.putString(keyName, value)
            is Boolean -> editor.putBoolean(keyName, value)
            is Float -> editor.putFloat(keyName, value)
            is Int -> editor.putInt(keyName, value)
            is Long -> editor.putLong(keyName, value)
            else -> editor.putString(keyName, gson.toJson(value))
        }
        editor.apply()
    }

    override fun <T> putList(keyName: String, value: List<T>) {
        sharedPreferences.edit().putString(keyName, gson.toJson(value)).apply()
    }

    override fun <T> get(keyName: String, clazz: Class<T>, default: T?): T? {
        return when (clazz) {
            String::class.java -> sharedPreferences.getString(keyName, default as? String) as? T
            Boolean::class.java -> sharedPreferences.getBoolean(
                keyName,
                default as? Boolean ?: false
            ) as? T
            Float::class.java -> sharedPreferences.getFloat(keyName, default as? Float ?: 0f) as? T
            Int::class.java -> sharedPreferences.getInt(keyName, default as? Int ?: 0) as? T
            Long::class.java -> sharedPreferences.getLong(keyName, default as? Long ?: 0L) as? T
            else -> gson.fromJson(sharedPreferences.getString(keyName, default as? String), clazz)
        }
    }

    override fun <T> getList(keyName: String, clazz: Class<T>): List<T>? {
        val type = TypeToken.getParameterized(List::class.java, clazz).type
        return gson.fromJson<List<T>>(get(keyName, String::class.java), type)
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    override fun remove(keyName: String) {
        sharedPreferences.edit().remove(keyName).apply()
    }

    companion object {
        const val KEY_SHARE_PREF = "MyPrefs"
    }
}