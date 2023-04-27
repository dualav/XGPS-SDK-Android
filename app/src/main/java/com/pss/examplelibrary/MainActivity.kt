package com.pss.examplelibrary

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pss.barlibrary.CustomBar.Companion.setContrastBar
import com.pss.barlibrary.CustomBar.Companion.setTransparentBar
import com.pss.library.ShowLibrary.Companion.sToast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTransparentBar(this)
        sToast(this, "테스트 Toast")
    }
}