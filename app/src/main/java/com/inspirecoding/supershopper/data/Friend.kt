package com.inspirecoding.supershopper.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Friend(
    var id: String = "",
    var friendId: String = "",
    var friendName: String = "",
    var friendshipOwnerId: String = ""
): Parcelable
