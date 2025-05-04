package com.contact_app.contact.model

import com.contact_app.contact.base.dateToString
import com.contact_app.contact.base.stringToDate
import java.io.Serial
import java.io.Serializable
import java.util.Date


data class Contact(
    val id: Int? = null,
    val firstName: String? = "",
    val lastName: String? = "",
    val email: String? = "",
    val phone: String? = "",
    val company: String? = "",
    val address: String? = "",
    val createdAt: Date? = null,
    val gender: String? = "male",
    var isLongClicked: Boolean? = false,
    var birthday: Date? = null,
    var isFirstOfChar: Boolean? = false,
    var keySearch: String? = null
) : Serializable {
    fun getBirthContact(): String? {
        return dateToString(this.birthday)
    }

    fun setBirthContact(date:String?){
        this.birthday = stringToDate(date)
    }
}