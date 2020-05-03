package com.wallhaven

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.wallhaven.Functions.Companion.getPerm
import com.wallhaven.Functions.Companion.logText
import com.wallhaven.Functions.Companion.t
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
//todo canvas:trying to draw too large exception

var run: Boolean = false
var refreshTime: Long = 300
var oldRefreshTime: Long = refreshTime
var timerTime: Long = 1000
var starttime: Long = 0

var handler: Handler = Handler()
var context: Context? = null
var refresher: Runnable = Runnable {
    oldRefreshTime = refreshTime
    starttime = System.nanoTime()
    try {
        changeWallpaper()
    } catch (e: Exception) {
        e.printStackTrace()
        t(e.javaClass.name)
    }
}
var refreshTimer: Runnable = object : Runnable {
    override fun run() {
        timer!!.text = (oldRefreshTime - (System.nanoTime() - starttime) / 1000000000).toString()
        handler.postDelayed(this, timerTime)
    }
}
var img: ImageView? = null
var logTextView: TextView? = null
var timer: TextView? = null

var wallpaperManager: WallpaperManager? = null
var sharedPreferences: SharedPreferences? = null

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//todo  Intent(this, WallhavenService::class.java).also { intent -> startService(intent) }
        getPerm(this)
        initVar()
        logTextView!!.movementMethod = ScrollingMovementMethod()
        img!!.setImageDrawable(wallpaperManager!!.drawable)

        run = sharedPreferences!!.getBoolean("run", false)
        if (run) {
            findViewById<Button>(R.id.start).text = "(Running)STOP"
            if (!handler.hasCallbacks(refresher))
                handler.post(refresher)
            if (!handler.hasCallbacks(refreshTimer))
                handler.post(refreshTimer)
        } else {
            findViewById<Button>(R.id.start).text = "(Stopped)START"
            handler.removeCallbacks(refreshTimer)
            handler.removeCallbacks(refresher)
        }
    }

    private fun initVar() {
        context = applicationContext
        wallpaperManager = WallpaperManager.getInstance(context)
        sharedPreferences = getDefaultSharedPreferences(context)
        img = findViewById(R.id.img)
        timer = findViewById(R.id.timer)
        logTextView = findViewById(R.id.text)
    }

    override fun onResume() {
        super.onResume()
        val url = API().getURL()
        findViewById<TextView>(R.id.url).text = "$url($refreshTime s)"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    logText("Read Permission Given", logTextView)
                } else {
                    logText("Read Permission Denied", logTextView)
                }
                return
            }
            2 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    logText("Write Permission Given", logTextView)
                } else {
                    logText("Write Permission Denied", logTextView)
                }
                return
            }
            else -> {
            }
        }
    }

    companion object {
        fun setWallpaper(home: Bitmap?, lock: Bitmap?) {
            logText("Setting wallpaper", logTextView)
            Functions.SetWallpaper(home, lock).execute()
        }

        fun changeWallpaper() {
            val param = API().getURL()
            if (run)
                if (isSetting) {
                    val msg = "Already applying one"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    logText("----" + (Calendar.getInstance() as GregorianCalendar).toZonedDateTime().toLocalTime() + "----", logTextView)
                    API().getSet(param)
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
            }
            R.id.img -> {
                //todo download to downloads folder
            }
            R.id.cache -> {
                //todo
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