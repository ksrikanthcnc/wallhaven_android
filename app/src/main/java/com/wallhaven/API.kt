package com.wallhaven

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import com.wallhaven.Functions.Companion.t
import com.wallhaven.Functions.Companion.unsetting
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Semaphore
import kotlin.math.max

const val APIString: String = "https://wallhaven.cc/api/v1/search?"
var isSetting: Boolean = false
val mutex: Semaphore = Semaphore(1, true)

class API {
    fun getSet(url: String) {
        GetJSON(url).execute()
    }

    private fun Boolean.toInt() = if (this) '1' else '0'
    fun getURL(): String {
        val userSearch = sharedPreferences!!.getString("search", "")!!
        var search = ""
        if (userSearch.isNotBlank()) {
            search = "&q=$userSearch"
        }

        val categoriesSet = sharedPreferences!!.getStringSet("categories", hashSetOf())!!
        val general = categoriesSet.contains("general")
        val anime = categoriesSet.contains("anime")
        val people = categoriesSet.contains("people")
        val categories = "&categories=" + general.toInt() + anime.toInt() + people.toInt()

        val puritySet = sharedPreferences!!.getStringSet("purity", hashSetOf())!!
        var sfw = puritySet.contains("sfw")
        val sketchy = puritySet.contains("sketchy")
        val nsfw = puritySet.contains("nsfw")
        /* API issue, else fetching &purity=111 */
        if (!(sfw || sketchy || nsfw))
            sfw = true
        val purity = "&purity=" + sfw.toInt() + sketchy.toInt() + nsfw.toInt()

        val sortingSet = sharedPreferences!!.getStringSet("sorting", hashSetOf())!!
        var sorting = ""
        if (sortingSet.isNotEmpty()) {
            val randomSorter = sortingSet.shuffled().first()
            sorting = "&sorting=$randomSorter"
        } else {
            //todo remove
            sorting = "&sorting=random"
        }

        val userOrder = sharedPreferences!!.getString("order", "")!!
        var order = ""
        if (userOrder.isNotEmpty()) {
            order = "&order=$userOrder"
        }

        val userTopRange = sharedPreferences!!.getString("toprange", "")!!
        var toprange = ""
        if (userTopRange.isNotEmpty()) {
            toprange = "&topRange=$userTopRange"
        }

        val userMinRes = sharedPreferences!!.getString("atleast", "")!!
        var atleast = ""
        if (userMinRes.isNotBlank()) {
            atleast = "&atleast=$userMinRes"
        }

        val userResolutions = sharedPreferences!!.getString("resolutions", "")!!
        var resolutions = ""
        if (userResolutions.isNotBlank()) {
            resolutions = "&resolutions=$userResolutions"
        }

        val userRatios = sharedPreferences!!.getString("ratios", "")!!
        var ratios = ""
        if (userRatios.isNotBlank()) {
            ratios = "&ratios=$userRatios"
        }

        val userColors = sharedPreferences!!.getString("colors", "")!!
        var colors = ""
        if (userColors.isNotBlank()) {
            colors = "&colors=$userColors"
        }

        val userPage = sharedPreferences!!.getString("page", "")!!
        var page = ""
        if (userPage.isNotBlank()) {
            page = "&page=$userPage"
        }

        val userSeed = sharedPreferences!!.getString("ratios", "")!!
        var seed = ""
        if (userSeed.isNotBlank()) {
            seed = "&seed=$userSeed"
        }

        val userAPIKEY = sharedPreferences!!.getString("apikey", "")!!
        var apikey = ""
        if (userAPIKEY.isNotBlank()) {
            apikey = "&apikey=$userAPIKEY"
        }

        val param = "$search$categories$purity$sorting$order$toprange$atleast$resolutions$ratios$colors$page$seed$apikey"
        refreshTime = sharedPreferences!!.getString("time", "300")!!.toLong()

        return APIString + param
    }

    class GetJSON(private var URL: String) : AsyncTask<Void?, Void?, String?>() {
        var err = ""
        override fun onPreExecute() {
            mutex.acquire()
            isSetting = true
            Functions.logText("[$URL]JSON", logTextView)
        }

        override fun doInBackground(vararg urls: Void?): String? {
            return try {
                val url = URL(URL)
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
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                err = e.javaClass.name + "\n" + "[Check Params]"
                unsetting()
                null
            } catch (e: Exception) {
                e.printStackTrace()
                err = e.javaClass.name
                null
            }
        }

        override fun onPostExecute(response: String?) {
            if (response != null) {
                val json = JSONObject(response)
                val data = json.get("data") as JSONArray
                if (data.length() == 0) {
                    t("No images found")
                    unsetting()
                } else {
                    val all = ArrayList<String>()
                    for (i in 0..json.length()) {
                        val obj = data.get(i) as JSONObject
                        val path = obj.get("path") as String
                        all.add(path)
                    }
                    val url: String = all[0]
                    GetIMG(url).execute()
                }
            } else {
                t(err)
            }
        }
    }

    class GetIMG(private var uri: String) : AsyncTask<Void?, Void?, Bitmap?>() {
        var err = ""
        override fun onPreExecute() {
            Functions.logText("[$uri]Downloading", logTextView)
        }

        override fun doInBackground(vararg params: Void?): Bitmap? {
            val url = URL(uri)
            return try {
                BitmapFactory.decodeStream(url.openConnection().getInputStream())
            } catch (e: Exception) {
                err = e.javaClass.name
                null
            }
        }

        override fun onPostExecute(image: Bitmap?) {
            if (image != null) {
                val M = max(image.width, image.height)

                val cropped = Functions.cropToSquare(image)

//                val dimBy = sharedPreferences!!.getInt("dim", 0)
//                val dimmed = Functions.darkenBitMap(cropped, dimBy)
                var blurBy: Float = sharedPreferences!!.getInt("blur", 0).toFloat()
                if (blurBy == 0f)
                    blurBy = 1f
                blurBy /= 5f
                val blurred = Functions.blur(cropped, blurBy)
                val bg = Bitmap.createScaledBitmap(blurred, M, M, false)

                val bitmap = Functions.overlay(image, bg)

                if (!(sharedPreferences!!.getStringSet("where", hashSetOf())?.contains("home")!!
                            || sharedPreferences!!.getStringSet("where", hashSetOf())?.contains("lock")!!)
                ) {
                    Functions.logText("Using default home", logTextView)
                    sharedPreferences!!.edit().putStringSet("where", hashSetOf("home")).apply()
                }
                MainActivity.setWallpaper(bitmap, blurred)
                img?.setImageBitmap(bitmap)
            } else {
                t(err)
            }
        }
    }
}