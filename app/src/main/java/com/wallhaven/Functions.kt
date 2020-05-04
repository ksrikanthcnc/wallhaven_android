package com.wallhaven

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.WallpaperManager
import android.content.Context
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
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        const val dev: Boolean = false
        fun unsetting() {
            isSetting = false
            mutex.release()
            handler.postDelayed(refresher, refreshTime * 1000)
        }

        fun t(msg: String, dur: Int) {
            l("[Toast][$msg]", 4)
            Toast.makeText(MyApp.context, msg, dur).show()
        }

        fun t(msg: String) {
            l("[Toast][$msg]", 4)
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
            // if src==bg (optimization), Canvas throws immutable object error
            val final = Bitmap.createBitmap(bg)
            val canvas = Canvas(final)
            canvas.drawBitmap(src, (bg.width - src.width) / 2f, (bg.height - src.height) / 2f, null)
            return final
        }

        fun l(new: String) {
            l(new, 3)
        }

        fun l(new: String, th: Int) {
            val file = Thread.currentThread().stackTrace[th].fileName
            val method = Thread.currentThread().stackTrace[th].methodName
            val line = Thread.currentThread().stackTrace[th].lineNumber
            val str = "[$file][$method($line)]:[$new]"
            Log.d("[LoggingText]", str)
        }

        fun logText(toLog: String, text: TextView?) {
            val file = Thread.currentThread().stackTrace[3].fileName
//            val classname = Thread.currentThread().stackTrace[3].className
            val method = Thread.currentThread().stackTrace[3].methodName
            val line = Thread.currentThread().stackTrace[3].lineNumber
            val str = "[$file][$method($line)]:[$toLog]"
            Log.d("[LoggingText]", str)

            log += "[$toLog]\n"
            try {
                text!!.text = log
                sharedPreferences!!.edit().putString("log", logTextView!!.text.toString()).apply()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
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
    }

    class SetWallpaper(private val home: Bitmap?, private val lock: Bitmap?) : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            if (sharedPreferences!!.getStringSet("where", hashSetOf())?.contains("home")!!)
                wallpaperManager!!.setBitmap(home, null, false, WallpaperManager.FLAG_SYSTEM)
            if (sharedPreferences!!.getStringSet("where", hashSetOf())?.contains("lock")!!)
                wallpaperManager!!.setBitmap(lock, null, false, WallpaperManager.FLAG_SYSTEM)
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            unsetting()
        }
    }
}