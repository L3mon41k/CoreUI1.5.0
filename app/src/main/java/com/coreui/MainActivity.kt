package com.coreui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
        }
        root.addView(TextView(this).apply {
            text = "CoreUI 1.5.0"
            textSize = 28f
            gravity = Gravity.CENTER_VERTICAL
        }, ViewGroup.LayoutParams(-1, 64))

        root.addView(Button(this).apply {
            text = "Камера"
            setOnClickListener { startActivity(Intent(this@MainActivity, CameraActivity::class.java)) }
        }, ViewGroup.LayoutParams(-1, 56))

        root.addView(TextView(this).apply {
            text = "Приложения"
            textSize = 20f
            setPadding(0, 20, 0, 12)
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(query, PackageManager.MATCH_ALL)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        apps.forEach { info ->
            val label = info.loadLabel(packageManager).toString()
            val appIntent = packageManager.getLaunchIntentForPackage(info.activityInfo.packageName)
            if (appIntent != null && info.activityInfo.packageName != packageName) {
                list.addView(Button(this).apply {
                    text = label
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setOnClickListener { startActivity(appIntent) }
                }, ViewGroup.LayoutParams(-1, 52))
            }
        }
        setContentView(root)
    }
}