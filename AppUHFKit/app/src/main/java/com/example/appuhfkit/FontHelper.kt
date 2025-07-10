package com.example.appuhfkit

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView

object FontHelper {
    private var sukhumvitBold: Typeface? = null
    private var sukhumvitMedium: Typeface? = null

    fun getSukhumvitBold(context: Context): Typeface {
        if (sukhumvitBold == null) {
            sukhumvitBold = Typeface.createFromAsset(context.assets, "fonts/SukhumvitSet-Bold.ttf")
        }
        return sukhumvitBold!!
    }

    fun getSukhumvitMedium(context: Context): Typeface {
        if (sukhumvitMedium == null) {
            sukhumvitMedium = Typeface.createFromAsset(context.assets, "fonts/SukhumvitSet-Medium.ttf")
        }
        return sukhumvitMedium!!
    }

    fun applySukhumvitBold(textView: TextView) {
        textView.typeface = getSukhumvitBold(textView.context)
    }

    fun applySukhumvitMedium(textView: TextView) {
        textView.typeface = getSukhumvitMedium(textView.context)
    }
} 