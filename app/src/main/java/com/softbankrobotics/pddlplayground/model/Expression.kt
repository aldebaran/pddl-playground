package com.softbankrobotics.pddlplayground.model

import android.os.Parcel
import android.os.Parcelable
import com.softbankrobotics.pddlplayground.util.PDDLCategory

class Expression(
    private var id: Long,
    private var label: String?,
    private var category: Int,
    private var enabled: Boolean
): Parcelable {
    private constructor(`in`: Parcel) : this(
        id = `in`.readLong(),
        label = `in`.readString(),
        category = `in`.readInt(),
        enabled = `in`.readBoolean()
    )
    constructor() : this(NO_ID)
    constructor(id: Long) : this(id, "")
    constructor(id: Long, label: String) : this(id, label, PDDLCategory.TYPE.ordinal)
    constructor(id: Long, label: String, category: Int) : this(id, label, category, true)


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(label)
        parcel.writeInt(category)
        parcel.writeByte(if (enabled) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getId(): Long {
        return id
    }

    fun getLabel(): String? {
        return label
    }

    fun getCategory(): Int {
        return category
    }

    fun isEnabled(): Boolean {
        return enabled
    }

    fun setLabel(label: String) {
        this.label = label
    }

    fun setCategory(category: Int) {
        this.category = category
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    companion object CREATOR : Parcelable.Creator<Expression> {
        override fun createFromParcel(parcel: Parcel): Expression {
            return Expression(parcel)
        }

        override fun newArray(size: Int): Array<Expression?> {
            return arrayOfNulls(size)
        }

        private const val NO_ID: Long = -1
    }
}