package com.nevruz.aegis.core

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.nevruz.aegis.core.service.MonitorService

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "AEGIS"
            textSize = 28f
            gravity = Gravity.CENTER
        }
        val status = TextView(this).apply {
            text = "Koruma servisi kapalı"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        val start = Button(this).apply {
            text = "Korumayı başlat"
            setOnClickListener {
                val intent = Intent(this@MainActivity, MonitorService::class.java)
                    .setAction(MonitorService.ACTION_START)
                startForegroundService(intent)
                status.text = "Koruma servisi çalışıyor"
            }
        }
        val stop = Button(this).apply {
            text = "Korumayı durdur"
            setOnClickListener {
                stopService(Intent(this@MainActivity, MonitorService::class.java))
                status.text = "Koruma servisi kapalı"
            }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            addView(title)
            addView(status)
            addView(start)
            addView(stop)
        })
    }
}
