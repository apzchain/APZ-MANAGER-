package com.apz.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.apz.manager.ui.APZManagerScreen
import com.apz.manager.ui.theme.APZTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { APZTheme { APZManagerScreen() } }
    }
}
