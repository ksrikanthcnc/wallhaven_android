package com.wallhaven

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ThumbnailUtils
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min


class MyApp : Application() {
    override fun onCreate() {
        instance = this
        super.onCreate()
    }

    companion object {
        var instance: MyApp? = null
            private set

        // or return instance.getApplicationContext();
        val context: Context?
            get() = instance
        // or return instance.getApplicationContext();
    }
}

class Functions {
    companion object {
        fun unsetting() {
            isSetting = false
            mutex.release()
            if (sharedPreferences!!.getBoolean("run", false))
                handler.postDelayed(refresher, refreshTime * 1000)
        }

        fun l(new: String, th: Int) {
            val file = Thread.currentThread().stackTrace[th].fileName
            val classname = "" //Thread.currentThread().stackTrace[th].className
            val method = Thread.currentThread().stackTrace[th].methodName
            val line = Thread.currentThread().stackTrace[th].lineNumber
//            val str = ".($file:$line)[$file$classname][$method($line)]:[$new]"
//            Log.d("[LoggingText]", str)
            val str = "[Logging].($file:$line):[$new]"
            println(str)
        }

        fun logText(strToLog: String) {
            logText(strToLog, false)
        }

        fun logText(strToLog: String, showToast: Boolean) {
            if (showToast) {
                l("Toast:$strToLog", 4)
                t(strToLog)
            } else {
                l(strToLog, 5)
            }
            var log = sharedPreferences!!.getString("log", "")
            val toLog = strToLog.removePrefix(APIString)
            val simpleDateFormat = SimpleDateFormat("hh:mm:ss", Locale.getDefault())
            val time = simpleDateFormat.format(Date())
//            val time = (Calendar.getInstance() as GregorianCalendar).toZonedDateTime().toLocalTime().withNano(0).withSecond(2)
            log += "[$time][$toLog]\n"
            sharedPreferences!!.edit().putString("log", log).apply()
        }

//        fun t(msg: String, dur: Int) {
//            l("[Toast][$msg]", 4)
//            Toast.makeText(MyApp.context, msg, dur).show()
//        }

        fun t(msg: String) {
//            Toast.makeText(MyApp.context, msg, Toast.LENGTH_LONG).show()
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } else {
                Handler(Looper.getMainLooper()).post(Runnable { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() })
            }
        }

        fun blur(image: Bitmap, blurBy: Float): Bitmap {
            val renderScript: RenderScript = RenderScript.create(context);
            val input: Allocation = Allocation.createFromBitmap(renderScript, image)
            val output: Allocation = Allocation.createTyped(renderScript, input.type);
            val script: ScriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            script.setRadius(blurBy);
            script.setInput(input);
            script.forEach(output);
            output.copyTo(image);
            return image
        }

        fun darkenBitMap(bitmap: Bitmap, dimBy: Int): Bitmap? {
            //todo dim
            val canvas = Canvas(bitmap)
            canvas.drawARGB(dimBy, 0, 0, 0)
            canvas.drawBitmap(bitmap, Matrix(), Paint())
            return bitmap
        }

        fun cropToSquare(bitmap: Bitmap): Bitmap {
            val side = min(bitmap.width, bitmap.height)
            return ThumbnailUtils.extractThumbnail(bitmap, side, side)
        }

        fun overlay(src: Bitmap, bg: Bitmap): Bitmap {
            val final = Bitmap.createBitmap(bg)
            val canvas = Canvas(final)
            canvas.drawBitmap(src, (bg.width - src.width) / 2f, (bg.height - src.height) / 2f, null)
            return final
        }

        fun getPerm(activity: Activity) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1
                )
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    2
                )
            }
        }

        fun jsonToString(jsonURL: JSONObject): String {
            return "${jsonURL.get("API")}${jsonURL.get("search")}${jsonURL.get("categories")}${jsonURL.get("purity")}${jsonURL.get("sorting")}" +
                    "${jsonURL.get("order")}" + "${jsonURL.get("toprange")}${jsonURL.get("atleast")}${jsonURL.get("resolutions")}${jsonURL.get("ratios")}" +
                    "${jsonURL.get("colors")}" + "${jsonURL.get("page")}${jsonURL.get("seed")}${jsonURL.get("apikey")}"
        }

        fun updateUrlLog() {
            val left = sharedPreferences!!.getStringSet("urls", hashSetOf())!!.size
            val total = sharedPreferences!!.getString("total", "")!!
            val param = url.removePrefix(APIString)
            urlText!!.text = "[$APIString]\n[$param]@${refreshTime}s[${left}left][${total}total]"
            logTextView!!.text = sharedPreferences!!.getString("log", "")
        }

//        fun get(key: String): () -> Any {
//            val p: SharedPreferences = sharedPreferences!!
//            return {
//                when (key) {
//                    "latest" -> p.getString(key, "")
//                    "url" -> p.getString(key, "")
//                    "refreshtime" -> p.getString(key, "")
//                    "log" -> p.getString(key, "")
//                    "oldurl" -> p.getString(key, "")
//                    "refreshpage" -> p.getBoolean(key, true)
//                    "updatepage" -> p.getBoolean(key, true)
//                    "run" -> p.getBoolean(key, false)
//                    "urls" -> p.getStringSet(key, hashSetOf())
//                    "search" -> p.getString(key, "")
//                    "categoriesset" -> p.getStringSet(key, hashSetOf())
//                    "purityset" -> p.getStringSet(key, hashSetOf())
//                    "oldsortingset" -> p.getStringSet(key, hashSetOf())
//                    "sortingset" -> p.getStringSet(key, hashSetOf())
//                    "sorting" -> p.getString(key, "")
//                    "order" -> p.getString(key, "")
//                    "toprange" -> p.getString(key, "")
//                    "atleast" -> p.getString(key, "")
//                    "resolutions" -> p.getString(key, "")
//                    "ratios" -> p.getString(key, "")
//                    "colors" -> p.getString(key, "")
//                    "page" -> p.getString(key, "")
//                    "seed" -> p.getString(key, "")
//                    "apikey" -> p.getString(key, "")
//                    "index" -> p.getInt(key, 1)
//                    "total" -> p.getString(key, "")
//                    "blur" -> p.getInt(key, 1)
//                    "dim" -> p.getInt(key, 1)
//                    "where" -> p.getString(key, "")
//
//                    else -> throw Exception("No pref found")
//                }!!
//            }
//
//        }
    }

    class SetWallpaper(private val home: Bitmap?, private val lock: Bitmap?) : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            //todo bind latest from here
            if ((sharedPreferences!!.getStringSet("where", hashSetOf())!!.contains("home")
                        || sharedPreferences!!.getStringSet("where", hashSetOf())!!.contains("lock"))
            ) {
                logText("Setting wallpaper")
            }
            if (sharedPreferences!!.getStringSet("where", hashSetOf())?.contains("home")!!)
                wallpaperManager!!.setBitmap(home, null, false, WallpaperManager.FLAG_SYSTEM)
            if (sharedPreferences!!.getStringSet("where", hashSetOf())?.contains("lock")!!)
                wallpaperManager!!.setBitmap(lock, null, false, WallpaperManager.FLAG_SYSTEM)
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            if ((sharedPreferences!!.getStringSet("where", hashSetOf())!!.contains("home")
                        || sharedPreferences!!.getStringSet("where", hashSetOf())!!.contains("lock"))
            ) {
                logText("Done")
                t("Wallpaper(s) updated")
            }
            unsetting()
        }
    }
}