package com.contact_app.contact.model

import com.contact_app.contact.base.dateToString
import com.contact_app.contact.base.stringToDate
import java.io.Serial
import java.io.Serializable
import java.util.Date


data class Contact(
    var id: Int? = null,
    var firstName: String? = "",
    var lastName: String? = "",
    var email: String? = "",
    var phone: String? = "",
    var company: String? = "",
    var address: String? = "",
    var createdAt: Date? = null,
    var gender: String? = "male",
    var isLongClicked: Boolean? = false,
    var birthday: Date? = null,
    var isFirstOfChar: Boolean? = false,
    var keySearch: String? = null,
    var isChecked: Boolean? = false
) : Serializable {
    fun getBirthContact(): String? {
        return dateToString(this.birthday)
    }

    fun setBirthContact(date: String?) {
        this.birthday = stringToDate(date)
    }

    fun getFullName(): String{
        return "$firstName $lastName".trim()
    }
}