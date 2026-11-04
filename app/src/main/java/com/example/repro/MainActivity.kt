package com.example.repro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var flag by remember { mutableStateOf(false) }
            var count by remember { mutableIntStateOf(0) }
            Column(Modifier.padding(32.dp)) {
                BasicText("Toggle flag", Modifier.clickable { flag = !flag }.padding(16.dp))
                BasicText("Increment count", Modifier.clickable { count++ }.padding(16.dp))
                BasicText("expected: flag=$flag count=$count", Modifier.padding(16.dp))
                Repro(flag, count)
            }
        }
    }
}
