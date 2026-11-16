package com.dff.ular

import android.app.*
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.view.WindowManager
import java.text.SimpleDateFormat
import java.util.*

class SnakeService : Service() {
    companion object {
        var instance: SnakeService? = null
        const val HELP = "Perintah:\nbuka <aplikasi>\nmusik <judul/artis>\nalarm 07:30 <pesan>\ntimer 5 | timer 30 detik\njadwal [besok] 17:00 <judul>\ncatat <teks> | lihat catatan | hapus catatan\nwarna biru|merah|hijau|kuning|ungu|oranye\njam | stop"
    }

    private var view: SnakeView? = null
    private lateinit var wm: WindowManager

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val ch = NotificationChannel("ular", "Ular Asisten", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        val n = notif()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
        val f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            f, PixelFormat.TRANSLUCENT
        )
        view = SnakeView(this).also { wm.addView(it, lp) }
        view?.say("Halo! Aku ular asistenmu")
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (i != null && i.action == "REPLY") {
            RemoteInput.getResultsFromIntent(i)?.getCharSequence("cmd")?.let { handle(it.toString()) }
            getSystemService(NotificationManager::class.java).notify(1, notif())
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        instance = null
        view?.let { wm.removeView(it) }
        view = null
        super.onDestroy()
    }

    private fun notif(): Notification {
        val ri = RemoteInput.Builder("cmd").setLabel("Ketik perintah...").build()
        val pi = PendingIntent.getService(
            this, 1, Intent(this, SnakeService::class.java).setAction("REPLY"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val act = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_send), "Perintah", pi
        ).addRemoteInput(ri).build()
        return Notification.Builder(this, "ular")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Ular aktif")
            .setContentText("Tekan 'Perintah' untuk mengetik")
            .addAction(act).setOngoing(true).build()
    }

    private fun prefs() = getSharedPreferences("ular", MODE_PRIVATE)

    private fun go(i: Intent) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    fun handle(raw: String) {
        val t = raw.trim()
        val l = t.lowercase()
        val rest = t.substringAfter(" ", "").trim()
        val out = try {
            when {
                l.startsWith("buka ") -> buka(rest)
                l.startsWith("musik ") || l.startsWith("putar ") -> musik(rest)
                l.startsWith("alarm ") -> alarm(rest)
                l.startsWith("timer ") -> timer(rest)
                l.startsWith("jadwal ") -> jadwal(rest)
                l.startsWith("catat ") -> {
                    val p = prefs()
                    p.edit().putString("n", p.getString("n", "") + rest + "\n").apply()
                    "Dicatat"
                }
                l == "lihat catatan" || l == "catatan" -> {
                    val x = prefs().getString("n", "")!!.trim()
                    if (x.isEmpty()) "Belum ada catatan"
                    else x.split("\n").mapIndexed { i, s -> "${i + 1}. $s" }.takeLast(8).joinToString("\n")
                }
                l == "hapus catatan" -> { prefs().edit().clear().apply(); "Catatan dihapus" }
                l.startsWith("warna ") -> warna(l.substringAfter(" ").trim())
                l == "jam" || l == "jam berapa" ->
                    SimpleDateFormat("HH:mm, EEEE d MMMM", Locale("id")).format(Date())
                l == "stop" || l == "matikan" -> { stopSelf(); "Dadah" }
                l == "bantuan" -> HELP
                else -> "Belum paham. Ketik: bantuan"
            }
        } catch (e: Exception) {
            "Gagal: ${e.message}"
        }
        view?.say(out)
    }

    private fun buka(nama: String): String {
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(q, 0)
        val n = nama.lowercase()
        val hit = apps.firstOrNull { it.loadLabel(packageManager).toString().lowercase() == n }
            ?: apps.firstOrNull { it.loadLabel(packageManager).toString().lowercase().contains(n) }
            ?: return "Aplikasi '$nama' tidak ketemu"
        go(packageManager.getLaunchIntentForPackage(hit.activityInfo.packageName) ?: return "Tidak bisa dibuka")
        return "Membuka ${hit.loadLabel(packageManager)}"
    }

    private fun musik(q: String): String {
        val i = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            .putExtra(SearchManager.QUERY, q)
        try {
            go(i)
        } catch (e: ActivityNotFoundException) {
            go(Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=" + Uri.encode(q))))
        }
        return "Memutar: $q"
    }

    private fun alarm(s: String): String {
        val m = Regex("^(\\d{1,2})[:.](\\d{2})\\s*(.*)$").find(s) ?: return "Format: alarm 07:30 bangun"
        val h = m.groupValues[1].toInt()
        val mi = m.groupValues[2].toInt()
        if (h > 23 || mi > 59) return "Jam tidak valid"
        go(
            Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, h)
                .putExtra(AlarmClock.EXTRA_MINUTES, mi)
                .putExtra(AlarmClock.EXTRA_MESSAGE, m.groupValues[3].ifEmpty { "Alarm ular" })
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        )
        return "Alarm $h:${m.groupValues[2]} dipasang"
    }

    private fun timer(s: String): String {
        val m = Regex("^(\\d+)\\s*(detik|menit|jam)?").find(s) ?: return "Format: timer 5 | timer 30 detik"
        val v = m.groupValues[1].toInt()
        val sec = when (m.groupValues[2]) { "detik" -> v; "jam" -> v * 3600; else -> v * 60 }
        go(
            Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, sec)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "Timer ular")
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        )
        return "Timer $sec detik dimulai"
    }

    private fun jadwal(s: String): String {
        val m = Regex("^(besok\\s+)?(\\d{1,2})[:.](\\d{2})\\s+(.+)$", RegexOption.IGNORE_CASE).find(s)
            ?: return "Format: jadwal [besok] 17:00 judul"
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, m.groupValues[2].toInt())
            set(Calendar.MINUTE, m.groupValues[3].toInt())
            set(Calendar.SECOND, 0)
            if (m.groupValues[1].isNotEmpty() || timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        go(
            Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, m.groupValues[4])
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, c.timeInMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, c.timeInMillis + 3600000L)
        )
        return "Jadwal siap, tekan Simpan di kalender"
    }

    private fun warna(w: String): String {
        val h = mapOf("merah" to 0f, "oranye" to 30f, "kuning" to 55f, "hijau" to 120f, "biru" to 220f, "ungu" to 280f)[w]
            ?: return "Pilih: merah, oranye, kuning, hijau, biru, ungu"
        view?.hue = h
        return "Warna $w"
    }
}
