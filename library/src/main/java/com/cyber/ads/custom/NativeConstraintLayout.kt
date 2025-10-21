package com.cyber.ads.custom

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.constraintlayout.widget.ConstraintLayout

class NativeConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        isFocusableInTouchMode = true
        requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

}
