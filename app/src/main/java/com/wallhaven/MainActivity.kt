package com.wallhaven

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.wallhaven.MainActivity.Companion.changeWallpaper
import com.wallhaven.MainActivity.Companion.logText
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList

const val API: String = "https://wallhaven.cc/api/v1/search?"
var run: Boolean = false
var isSetting: Boolean = false
val mutex: Semaphore = Semaphore(1, true)
var refreshTime: Long = 10
var timerTime: Long = 1000
var starttime: Long = 0

var handler: Handler = Handler()
var context: Context? = null
var refresher: Runnable = object : Runnable {
    override fun run() {
        changeWallpaper()
        starttime = System.nanoTime()
        handler.postDelayed(this, refreshTime * 1000)
    }
}
var timerer: Runnable = object : Runnable {
    override fun run() {
        timer!!.text = (refreshTime - (System.nanoTime() - starttime) / 1000000000).toString()
        handler.postDelayed(this, timerTime)
    }
}
var img: ImageView? = null
var text: TextView? = null
var timer: TextView? = null

var general: Boolean? = null
var anime: Boolean? = null
var people: Boolean? = null
var categories: String? = null
var sfw: Boolean? = null
var sketchy: Boolean? = null
var nsfw: Boolean? = null
var purity: String? = null

var wallpaperManager: WallpaperManager? = null
var sharedPreferences: SharedPreferences? = null
var log: String = ""

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                2
            )
        }

        context = applicationContext
        wallpaperManager = WallpaperManager.getInstance(context)
        img = findViewById(R.id.img)
        text = findViewById(R.id.text)
        text!!.movementMethod = ScrollingMovementMethod()
        timer = findViewById(R.id.timer)
        sharedPreferences = getDefaultSharedPreferences(context)

        img?.setImageDrawable(wallpaperManager!!.drawable)
        run = sharedPreferences!!.getBoolean("run", false)
        if (run) {
            findViewById<Button>(R.id.start).text = "(Running)STOP"
            if (!handler.hasCallbacks(refresher))
                handler.post(refresher)
            handler.post(timerer)
        } else {
            findViewById<Button>(R.id.start).text = "(Stopped)START"
            handler.removeCallbacks(timerer)
            handler.removeCallbacks(refresher)
        }
    }

    override fun onResume() {
        super.onResume()
        val param = getParam()
        logText("$param($refreshTime s)")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
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
        private fun Boolean.toInt() = if (this) '1' else '0'
        fun logText(new: String) {
            val file = Thread.currentThread().stackTrace[3].fileName
//            val classname = Thread.currentThread().stackTrace[3].className
            val method = Thread.currentThread().stackTrace[3].methodName
            val line = Thread.currentThread().stackTrace[3].lineNumber
            val str = "[$file][$method($line)]:[$new]"
            Log.d("[LoggingText]", str)

            val msg = "[$new]\n"
            try {
                text!!.append(msg)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        fun getParam(): String {
            //todo add more params
            general =
                sharedPreferences!!.getStringSet("categories", hashSetOf())?.contains("general")
            anime = sharedPreferences!!.getStringSet("categories", hashSetOf())?.contains("anime")
            people = sharedPreferences!!.getStringSet("categories", hashSetOf())?.contains("people")
            categories = "&categories=" + general!!.toInt() + anime!!.toInt() + people!!.toInt()

            sfw = sharedPreferences!!.getStringSet("purity", hashSetOf())?.contains("sfw")
            sketchy = sharedPreferences!!.getStringSet("purity", hashSetOf())?.contains("sketchy")
            nsfw = sharedPreferences!!.getStringSet("purity", hashSetOf())?.contains("nsfw")
            purity = "&purity=" + sfw!!.toInt() + sketchy!!.toInt() + false.toInt()

            val param = "$categories$purity&sorting=random"
            refreshTime = sharedPreferences!!.getString("time", "300")!!.toLong()
            return param
        }

        fun changeWallpaper() {
            val param = getParam()
            if (run)
                if (isSetting) {
                    val msg = "Already applying one"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    logText(
                        "----" + (Calendar.getInstance() as GregorianCalendar).toZonedDateTime()
                            .toLocalTime() + "----"
                    )
                    GetJSON(param).execute()
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
                    handler.post(timerer)
                } else {
                    findViewById<Button>(R.id.start).text = "(Stopped)START"
                    handler.removeCallbacks(timerer)
                    handler.removeCallbacks(refresher)
                }
            }
            R.id.clear -> {
                log = ""
                text!!.text = log
            }
            R.id.img -> {
                //todo download to downloads folder
            }
        }
    }
}

class GetJSON(private var param: String) :
    AsyncTask<Void?, Void?, String?>() {
    override fun onPreExecute() {
        mutex.acquire()
        isSetting = true
        logText("JSON[$API$param]")
    }

    override fun doInBackground(vararg urls: Void?): String? {
        return try {
            val url = URL(API + param)
            log += "JSON[$url]"
            val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.doOutput = false
            urlConnection.connect()
            try {
                val bufferedReader =
                    BufferedReader(InputStreamReader(urlConnection.inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    stringBuilder.append(line).append("\n")
                }
                bufferedReader.close()
                stringBuilder.toString()
            } finally {
                urlConnection.disconnect()
            }
        } catch (e: Exception) {
            logText(e.message.toString())
            null
        }
    }

    override fun onPostExecute(response: String?) {
        if (response != null) {
            val json = JSONObject(response)
            val data = json.get("data") as JSONArray
            val all = ArrayList<String>()
            for (i in 0..json.length()) {
                val obj = data.get(i) as JSONObject
                val path = obj.get("path") as String
                all.add(path)
            }
            val url: String = all[0]
            GetIMG(url).execute()
        }
    }
}

class GetIMG(private var uri: String) : AsyncTask<Void?, Void?, Bitmap?>() {
    override fun onPreExecute() {
        logText("Downloading[$uri]")
    }

    override fun doInBackground(vararg params: Void?): Bitmap? {
        val url = URL(uri)
        return try {
            BitmapFactory.decodeStream(url.openConnection().getInputStream())
            //todo blur effects
        } catch (e: java.lang.Exception) {
            logText(e.message.toString())
            null
        }
    }

    override fun onPostExecute(image: Bitmap?) {
        if (image != null) {
            SetWallpaper(image).execute()
            img?.setImageBitmap(image)
        }
        isSetting = false
        mutex.release()
    }
}

class SetWallpaper(private val image: Bitmap?) : AsyncTask<Void?, Void?, Void?>() {
    override fun onPreExecute() {
        logText("Setting wallpaper")
    }

    override fun doInBackground(vararg params: Void?): Void? {
        wallpaperManager!!.setBitmap(image, null, false, WallpaperManager.FLAG_SYSTEM)
        return null
    }
}