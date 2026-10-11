package com.potatoes.v2interp

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {
    private lateinit var etPkg: EditText
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etPkg = findViewById(R.id.etPkg)
        tvLog = findViewById(R.id.tvLog)
        val btnStart = findViewById<Button>(R.id.btnStart)

        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == 1001 && grantResult == PackageManager.PERMISSION_GRANTED) {
                runOnUiThread { tvLog.text = "Shizuku 授权成功，正在执行..." }
                executeInterp(etPkg.text.toString().trim())
            } else {
                runOnUiThread { tvLog.text = "❌ Shizuku 授权被拒绝" }
            }
        }

        btnStart.setOnClickListener {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                executeInterp(etPkg.text.toString().trim())
            } else {
                tvLog.text = "正在申请 Shizuku 权限..."
                Shizuku.requestPermission(1001)
            }
        }
    }

    private fun executeInterp(pkgName: String) {
        Thread {
            try {
                val pidCmd = "ps -A | grep $pkgName | awk '{print \$2}' | head -n 1"
                val pid = runShizukuCmd(pidCmd).trim()

                if (pid.isEmpty()) {
                    runOnUiThread { tvLog.text = "❌ 没找到进程，请确保游戏正在后台运行！" }
                    return@Thread
                }

                val interpCmd = "settings put system gamecube_frame_interpolation 1:3:$pid:45:90"
                runShizukuCmd(interpCmd)

                runOnUiThread { tvLog.text = "🟢 命令已发送！PID: $pid\n请立刻【锁屏，再解锁】回游戏！" }
            } catch (e: Exception) {
                runOnUiThread { tvLog.text = "❌ 出错: ${e.message}" }
            }
        }.start()
    }

    private fun runShizukuCmd(cmd: String): String {
        val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        return output
    }
}
