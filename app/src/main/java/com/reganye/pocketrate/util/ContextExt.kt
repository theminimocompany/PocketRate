package com.reganye.pocketrate.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Walks up the Context wrapper chain to find the hosting Activity, if any.
 * Returns null when the Context is not associated with an Activity (e.g. a Service).
 */
tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
