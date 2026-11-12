package id.example.smssync

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*

class MainActivity : Activity() {
    companion object {
        const val PREFS = "sms_sync"
        const val API_BASE_URL = "https://YOUR-SERVER-DOMAIN"
        const val REQUEST_SMS = 41
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 24)
        }
        layout.addView(TextView(this).apply {
            text = "SMS Sync untuk HP 2"
            textSize = 22f
        })
        layout.addView(TextView(this).apply {
            text = "Aplikasi ini mengirim SMS BARU yang masuk ke server milik Anda agar dapat dilihat di dashboard HP 1. Pemilik HP ini harus menyetujui izin SMS. Aplikasi tidak disembunyikan."
            textSize = 16f
        })
        val token = EditText(this).apply {
            hint = "Token perangkat dari administrator"
            setText(prefs.getString("device_token", ""))
        }
        layout.addView(token)
        val save = Button(this).apply { text = "Simpan token perangkat" }
        layout.addView(save)
        val permission = Button(this).apply { text = "Setujui izin SMS" }
        layout.addView(permission)
        val status = TextView(this).apply { textSize = 14f }
        layout.addView(status)
        save.setOnClickListener {
            prefs.edit().putString("device_token", token.text.toString().trim()).apply()
            status.text = "Token tersimpan di perangkat."
        }
        permission.setOnClickListener {
            if (token.text.toString().trim().isEmpty()) {
                status.text = "Masukkan dan simpan token perangkat terlebih dahulu."
            } else if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_SMS)
            } else status.text = "Izin SMS sudah aktif."
        }
        status.text = if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
            "Izin SMS aktif." else "Izin SMS belum diberikan."
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SMS) {
            Toast.makeText(this, if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
                "Izin diberikan" else "Izin tidak diberikan", Toast.LENGTH_LONG).show()
        }
    }
}
