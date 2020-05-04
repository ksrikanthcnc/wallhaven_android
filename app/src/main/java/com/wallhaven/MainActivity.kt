package com.wallhaven

import android.app.WallpaperManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.wallhaven.Functions.Companion.getPerm
import com.wallhaven.Functions.Companion.l
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
//todo canvas:trying to draw too large exception workaround:disable hardware acc

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
        urlText!!.text = "$url@($refreshTime s)[left in page:${sharedPreferences!!.getStringSet("urls", hashSetOf())!!.size}]"
        timer!!.text = (oldRefreshTime - (System.nanoTime() - starttime) / 1000000000).toString()
//        l(timer!!.text as String)
        logTextView!!.text = log
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
var log: String = ""

class MainActivity : AppCompatActivity() {
//    private lateinit var wallhavenService: WallhavenService
//    private var bound: Boolean = false
//
//    private val connection = object : ServiceConnection {
//        override fun onServiceConnected(className: ComponentName, service: IBinder) {
//            val binder = service as WallhavenService.LocalBinder
//            wallhavenService = binder.getService()
//            bound = true
//        }
//
//        override fun onServiceDisconnected(arg0: ComponentName) {
//            bound = false
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        Intent(this, WallhavenService::class.java).also { intent -> startService(intent) }
        getPerm(this)
        initVar()
        logTextView!!.movementMethod = ScrollingMovementMethod()
        img!!.setImageDrawable(wallpaperManager!!.drawable)
        latestTextView!!.text = sharedPreferences!!.getString("latest", "")

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
        allSet = true
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
    }

    override fun onResume() {
        super.onResume()
        //todo
        log += "$url"
        logTextView!!.text = sharedPreferences!!.getString("log", "")
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
            t("Wallpaper(s) updated")
            Functions.SetWallpaper(home, lock).execute()
        }

        fun changeWallpaper() {
            val jsonURL = API().getURL()
            if (run)
                if (isSetting) {
                    val msg = "Already applying one"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    logText("----" + (Calendar.getInstance() as GregorianCalendar).toZonedDateTime().toLocalTime() + "----", logTextView)
                    API().getSet(jsonURL)
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
                log = ""
                logTextView!!.text = ""
                sharedPreferences!!.edit().putString("log", "").apply()
            }
            R.id.img -> {
                //todo download to downloads folder
            }
//           todo
//           R.id.listcache -> {
//           }
            R.id.cache -> {
                sharedPreferences!!.edit().putStringSet("urls", hashSetOf()).apply()
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