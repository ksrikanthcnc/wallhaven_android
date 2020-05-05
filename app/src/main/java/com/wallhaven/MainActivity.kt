package com.wallhaven

import android.app.WallpaperManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.wallhaven.Functions.Companion.getPerm
import com.wallhaven.Functions.Companion.jsonToString
import com.wallhaven.Functions.Companion.l
import com.wallhaven.Functions.Companion.logText
import com.wallhaven.Functions.Companion.t
import com.wallhaven.Functions.Companion.updateUrlLog
import com.wallhaven.MainActivity.Companion.changeWallpaper
import java.lang.Exception
import java.util.*

//todo dont clear log
//todo nsfw (api issue)
//todo fix->log goes to bottom to each tick
//todo long press download, tap open in chrome
//todo about help, log, list
//todo user
//todo service background
//todo seed isnt working
//todo enhance api check page, seed, ...
//todo get settings with api, collections
//todo add effects
//todo start refresher after setwallpaper
//todo sleep if not used
//todo If not scrollable shrink to phone dimensions
//todo show pref in summary
//todo canvas:trying to draw too large exception workaround:disable hardware acc
//todo cache batch images [needed?]

var run: Boolean = false
var refreshTime: Long = 300
var oldRefreshTime: Long = refreshTime
var timerTime: Long = 100
var starttime: Long = 0

var handler: Handler = Handler()
var context: Context? = null
var refresher: Runnable = Runnable {
    oldRefreshTime = refreshTime
    starttime = System.nanoTime()
    changeWallpaper()
}
var refreshTimer: Runnable = object : Runnable {
    override fun run() {
        timer!!.text = (oldRefreshTime - (System.nanoTime() - starttime) / 1000000000).toString()
        updateUrlLog()
        handler.postDelayed(this, timerTime)
    }
}
var img: ImageView? = null
var urlText: TextView? = null
var logTextView: TextView? = null
var latestTextView: TextView? = null
var timer: TextView? = null

var wallpaperManager: WallpaperManager? = null
var sharedPreferences: SharedPreferences? = null

var allSet = false
var url: String = ""

var width = -1
var height = -1

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getPerm(this)
        initVar()
        logTextView!!.movementMethod = ScrollingMovementMethod()
        img!!.setImageDrawable(wallpaperManager!!.drawable)
        latestTextView!!.text = sharedPreferences!!.getString("latest", "")

        allSet = true
        logText(url)
    }

    override fun onResume() {
        super.onResume()
        val jsonURL = API().getURL()
        sharedPreferences!!.edit().putString("url", jsonToString(jsonURL)).apply()

        url = sharedPreferences!!.getString("url", "").toString()
        refreshTime = sharedPreferences!!.getString("refreshtime", "300")!!.toLong()
        val left = sharedPreferences!!.getStringSet("urls", hashSetOf())!!.size
        urlText!!.text = "$url@${refreshTime}s[${left}left]"
        logTextView!!.text = sharedPreferences!!.getString("log", "")

        if (url != sharedPreferences!!.getString("oldurl", "")) {
            sharedPreferences!!.edit().putString("oldurl", url).apply()
            sharedPreferences!!.edit().putBoolean("refreshpage", true).apply()
            logText(url)
        }

        run = sharedPreferences!!.getBoolean("run", false)
        run = !run
        btn(findViewById(R.id.start))

        updateUrlLog()
    }

    private fun initVar() {
        context = applicationContext
        wallpaperManager = WallpaperManager.getInstance(context)
        sharedPreferences = getDefaultSharedPreferences(context)
        img = findViewById(R.id.img)
        urlText = findViewById(R.id.url)
        timer = findViewById(R.id.timer)
        latestTextView = findViewById(R.id.latest)
        logTextView = findViewById(R.id.text)
        logTextView!!.movementMethod = ScrollingMovementMethod()

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        height = displayMetrics.heightPixels
        width = displayMetrics.widthPixels
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    logText("Read Permission Given")
                } else {
                    logText("Read Permission Denied")
                }
                return
            }
            2 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    logText("Write Permission Given")
                } else {
                    logText("Write Permission Denied")
                }
                return
            }
            else -> {
            }
        }
    }

    companion object {
        fun changeWallpaper() {
            val jsonURL = API().getURL()
            sharedPreferences!!.edit().putString("url", jsonToString(jsonURL)).apply()
            if (run)
                if (isSetting) {
                    val msg = "Already applying one"
                    logText(msg, true)
//                    t(msg)
                } else {
                    API.AGetSet(jsonURL).execute()
                }
        }
    }

    fun btn(view: View) {
        when (view.id) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.start -> {
                run = !run
                sharedPreferences!!.edit().putBoolean("run", run).apply()
                if (run) {
                    findViewById<Button>(R.id.start).text = "(Running)STOP"
                    handler.post(refresher)
                    handler.post(refreshTimer)
                } else {
                    findViewById<Button>(R.id.start).text = "(Stopped)START"
                    handler.removeCallbacks(refreshTimer)
                    handler.removeCallbacks(refresher)
                }
            }
            R.id.clear -> {
                logTextView!!.text = ""
                sharedPreferences!!.edit().putString("log", "").apply()
                updateUrlLog()
            }
            R.id.img -> {
                //todo download to downloads folder
            }
            R.id.cache -> {
                sharedPreferences!!.edit().putString("page", "1").apply()
                sharedPreferences!!.edit().putStringSet("urls", hashSetOf()).apply()
                sharedPreferences!!.edit().putBoolean("updatepage", false).apply()
                logText("Cache cleared", true)
                updateUrlLog()
//                sharedPreferences!!.edit().putString("page", "1").apply()
                //todo here now
            }
            R.id.refresh -> {
                run = true
                sharedPreferences!!.edit().putBoolean("run", run).apply()
                findViewById<Button>(R.id.start).text = "(Running)STOP"
                handler.removeCallbacks(refreshTimer)
                handler.removeCallbacks(refresher)
                handler.post(refresher)
                handler.post(refreshTimer)
            }
        }
    }
}