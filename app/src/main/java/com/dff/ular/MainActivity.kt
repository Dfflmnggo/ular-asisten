package com.dff.ular

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(p, p * 3, p, p)
        }
        setContentView(ScrollView(this).apply { addView(root) })
        fun tv(t: String) = TextView(this).apply { text = t; textSize = 15f }.also { root.addView(it) }
        fun btn(t: String, f: () -> Unit) =
            Button(this).apply { text = t; setOnClickListener { f() } }.also { root.addView(it) }

        tv("Ular Asisten\nLangkah 1-4 cukup sekali saja.")
        btn("1. Izinkan tampil di atas aplikasi lain") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        btn("2. Izinkan notifikasi") {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        btn("3. Tanpa batasan baterai") {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }
        btn("4. Bangunkan ular") {
            if (!Settings.canDrawOverlays(this)) toast("Selesaikan langkah 1 dulu")
            else startForegroundService(Intent(this, SnakeService::class.java))
        }
        btn("Matikan ular") { stopService(Intent(this, SnakeService::class.java)) }
        val et = EditText(this).apply { hint = "ketik perintah, mis: buka whatsapp"; isSingleLine = true }
        root.addView(et)
        btn("Kirim perintah") {
            val s = SnakeService.instance
            if (s == null) toast("Ular belum aktif")
            else { s.handle(et.text.toString()); et.setText("") }
        }
        tv(SnakeService.HELP)
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()
}
