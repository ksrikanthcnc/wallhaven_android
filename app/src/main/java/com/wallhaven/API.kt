package com.wallhaven

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.DisplayMetrics
import com.wallhaven.Functions.Companion.jsonToString
import com.wallhaven.Functions.Companion.logText
import com.wallhaven.Functions.Companion.overlay
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


//todo index [now]
const val APIString: String = "https://wallhaven.cc/api/v1/search?"
var isSetting: Boolean = false
val mutex: Semaphore = Semaphore(1, true)

class API {
    private fun Boolean.toInt() = if (this) '1' else '0'
    fun getURL(): JSONObject {
        val json = JSONObject()

        var search = ""
        var categories = ""
        var purity = ""
        var sorting = ""
        var order = ""
        var toprange = ""
        var atleast = ""
        var resolutions = ""
        var ratios = ""
        var colors = ""
        var page = ""
        var seed = ""
        var apikey = ""

        val userSearch = sharedPreferences!!.getString("search", "")!!
        if (userSearch.isNotBlank()) {
            search = "&q=$userSearch"
        }

        val categoriesSet = sharedPreferences!!.getStringSet("categoriesset", hashSetOf())!!
        if (categoriesSet.isEmpty()) {
            sharedPreferences!!.edit().putStringSet("categoriesset", hashSetOf("general")).apply()
            sharedPreferences!!.edit().putStringSet("categoriesset", hashSetOf("anime")).apply()
            sharedPreferences!!.edit().putStringSet("categoriesset", hashSetOf("people")).apply()
//            categoriesSet.add("general")
        } else {
            val general = categoriesSet.contains("general")
            val anime = categoriesSet.contains("anime")
            val people = categoriesSet.contains("people")
            categories = "&categories=" + general.toInt() + anime.toInt() + people.toInt()
        }

        val puritySet = sharedPreferences!!.getStringSet("purityset", hashSetOf())!!
        if (puritySet.isEmpty()) {
            sharedPreferences!!.edit().putStringSet("purityset", hashSetOf("sfw")).apply()
            //Else API gives nsfw too (api bug)
            puritySet.add("sfw")
        } else {
            val sfw = puritySet.contains("sfw")
            val sketchy = puritySet.contains("sketchy")
            val nsfw = puritySet.contains("nsfw")
            purity = "&purity=" + sfw.toInt() + sketchy.toInt() + nsfw.toInt()
        }

        val oldSortingSet = sharedPreferences!!.getStringSet("oldsortingset", hashSetOf())!!
        val sortingSet = sharedPreferences!!.getStringSet("sortingset", hashSetOf())!!
        if (sortingSet.size == 0 || oldSortingSet != sortingSet) {
            sharedPreferences!!.edit().putStringSet("oldsortingset", sortingSet).apply()
            if (sortingSet.isEmpty()) {
                sharedPreferences!!.edit().putStringSet("sortingset", hashSetOf("date_added")).apply()
//            sortingSet.add("date_added")
            } else {
                val randomSorter = sortingSet.shuffled().first()
                if (randomSorter != "date_added") {
                    sharedPreferences!!.edit().putString("sorting", randomSorter).apply()
                    sorting = "&sorting=$randomSorter"
                }
            }
        } else {
            sorting = "&sorting=${sharedPreferences!!.getString("sorting", "")}"
        }

        val userOrder = sharedPreferences!!.getString("order", "")!!
        if (userOrder.isBlank()) {
            sharedPreferences!!.edit().putString("order", "desc").apply()
//            userOrder = "desc"
        } else {
            if (userOrder != "desc")
                order = "&order=$userOrder"
        }

        if (sorting == "toplist") {
            val userTopRange = sharedPreferences!!.getString("toprange", "")!!
            if (userTopRange.isBlank()) {
                sharedPreferences!!.edit().putString("toprange", "1M").apply()
//            userTopRange = "1M"
            } else {
                if (userTopRange != "1M")
                    toprange = "&topRange=$userTopRange"
            }
        }

        val userMinRes = sharedPreferences!!.getString("atleast", "")!!
        if (userMinRes.isNotBlank()) {
            atleast = "&atleast=$userMinRes"
        }

        val userResolutions = sharedPreferences!!.getString("resolutions", "")!!
        if (userResolutions.isNotBlank()) {
            resolutions = "&resolutions=$userResolutions"
        }

        val userRatios = sharedPreferences!!.getString("ratios", "")!!
        if (userRatios.isNotBlank()) {
            ratios = "&ratios=$userRatios"
        }

        val userColors = sharedPreferences!!.getString("colors", "")!!
        if (userColors.isNotBlank()) {
            colors = "&colors=$userColors"
        }

        var sorter = sharedPreferences!!.getString("sorting", "")
        if (sorter == "toplist")
            sorter += "${sharedPreferences!!.getString("toprange", "1M")}"

        val currentPage = sharedPreferences!!.getString("page", "1")
        page = "&page=$currentPage"

        val userIndex = sharedPreferences!!.getInt("index", 1)
        var index = userIndex

        if (sorter == "random") {
            val userSeed = sharedPreferences!!.getString("seed", "")!!
            if (userSeed.isNotBlank()) {
                seed = "&seed=$userSeed"
            }
        }

        val userAPIKEY = sharedPreferences!!.getString("apikey", "")!!
        if (userAPIKEY.isNotBlank()) {
            apikey = "&apikey=$userAPIKEY"
        }

//        val param = "$search$categories$purity$sorting$order$toprange$atleast$resolutions$ratios$colors$page$seed$apikey"
        refreshTime = sharedPreferences!!.getString("refreshtime", "300")!!.toLong()

        //todo one liner using str replace
        json.put("API", APIString)
        json.put("search", search)
        json.put("categories", categories)
        json.put("purity", purity)
        json.put("sorting", sorting)
        json.put("order", order)
        json.put("toprange", toprange)
        json.put("atleast", atleast)
        json.put("resolutions", resolutions)
        json.put("ratios", ratios)
        json.put("colors", colors)
        json.put("page", page)
        json.put("seed", seed)
        json.put("apikey", apikey)
        return json
    }

    class AGetSet(private var jsonURL: JSONObject) : AsyncTask<Void?, Void?, Bitmap?>() {
        var err = ""
        override fun onPreExecute() {
            super.onPreExecute()
            mutex.acquire()
            isSetting = true
        }

        override fun doInBackground(vararg params: Void?): Bitmap? {
            if (sharedPreferences!!.getBoolean("refreshpage", true)) {
                logText("Refreshing cache(Param change detected)")
                sharedPreferences!!.edit().putBoolean("refreshpage", false).apply()
                sharedPreferences!!.edit().putStringSet("urls", hashSetOf()).apply()
            }
            var urls = sharedPreferences!!.getStringSet("urls", hashSetOf())!!
            url = jsonToString(jsonURL)
            if (urls.isEmpty()) {
                val msg: String
                val json = JSONObject(fetchJSONString(url)!!)
                val meta = json.get("meta") as JSONObject
                if (meta.get("total") == 0) {
                    msg = "No images with given params...Stopping"
                    sharedPreferences!!.edit().putBoolean("run", false).apply()
                } else {
                    sharedPreferences!!.edit().putString("total", meta.get("total").toString()).apply()
                    msg = "Refreshing cache list"
                    var newJson: JSONObject
                    do {
                        if (sharedPreferences!!.getBoolean("updatepage", true) && !sharedPreferences!!.getBoolean("refreshpage", true))
                            updatePage(json, jsonURL)
                        val newUrl = jsonToString(jsonURL)
//                    logText(newUrl)
                        newJson = JSONObject(fetchJSONString(newUrl)!!)
                        if ((newJson.get("data") as JSONArray).length() == 0)
                            sharedPreferences!!.edit().putBoolean("updatepage", true).apply()
                    } while ((newJson.get("data") as JSONArray).length() == 0)
                    val newUrls: Set<String> = dataToUrls(newJson.get("data") as JSONArray)
                    sharedPreferences!!.edit().putStringSet("urls", newUrls).apply()
                    urls = newUrls
                }
                t(msg)
                logText(msg)
            }
            return if (urls.isNotEmpty()) {
                val url = getFirstURL(urls)
                val image: Bitmap? = getIMG(url)
                image
            } else {
                null
            }
        }

        override fun onPostExecute(image: Bitmap?) {
            super.onPostExecute(image)
            if (image != null)
                setBitmap(image)
            unsetting()
        }

        private fun getFirstURL(urls: Set<String>): String {
            val url: String = urls.first()
            sharedPreferences!!.edit().putString("latest", "${jsonToString(jsonURL)}\n[$url]").apply()
            val newUrls = urls.drop(1).toSet()
            if (newUrls.isEmpty())
                sharedPreferences!!.edit().putBoolean("updatepage", true).apply()
            sharedPreferences!!.edit().putStringSet("urls", newUrls).apply()
            return url
        }

        private fun updatePage(json: JSONObject, jsonURL: JSONObject) {
            logText("Next Page")
            val meta = json.get("meta") as JSONObject
            val currentPage = meta.get("current_page")
            var page = (currentPage as Int) + 1
            val lastPage = meta.get("last_page") as Int
            if (page > lastPage) {
                logText("Last Page done, circling back to first")
                page = 1
            }
            jsonURL.put("page", "&page=$page")
            sharedPreferences!!.edit().putString("page", page.toString()).apply()
        }

        private fun dataToUrls(data: JSONArray): Set<String> {
            val urls = ArrayList<String>()
            for (i in 0 until data.length()) {
                val obj = data.get(i) as JSONObject
                val path = obj.get("path") as String
                urls.add(path)
            }
            return urls.toSet()
        }

        private fun fetchJSONString(URL: String): String? {
            return try {
                //todo throws error
                val url = URL(URL)
                val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.doOutput = false
                urlConnection.connect()
                try {
                    val bufferedReader = BufferedReader(InputStreamReader(urlConnection.inputStream))
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
                t(err)
                null
            } catch (e: Exception) {
                e.printStackTrace()
                err = e.javaClass.name
                t(err)
                null
            }
        }

        private fun getIMG(uri: String): Bitmap? {
            //todo error
            logText("$uri...${sharedPreferences!!.getStringSet("urls", hashSetOf())!!.size}more")
            val url = URL(uri)
            return try {
                BitmapFactory.decodeStream(url.openConnection().getInputStream())
            } catch (e: Exception) {
                err = e.javaClass.name
                logText("$err")
                t(err)
                null
            }
        }

        private fun setBitmap(image: Bitmap) {
            val side = max(image.width, image.height)
            var blurBy: Float = sharedPreferences!!.getInt("blur", 0).toFloat()
            if (blurBy == 0f)
                blurBy = 1f
            blurBy /= 5f
            try {
                val dimBy = sharedPreferences!!.getInt("dim", 0)

                val copy = image.copy(image.config, true)
                val cropped = Functions.cropToSquare(copy)
                val blurred = Functions.blur(cropped, blurBy)
                val dimmed = Functions.darkenBitMap(cropped, dimBy)
                val bg = Bitmap.createScaledBitmap(blurred, side, side, false)
                val bitmap = overlay(image, bg)

                if (!(sharedPreferences!!.getStringSet("where", hashSetOf())!!.contains("home")
                            || sharedPreferences!!.getStringSet("where", hashSetOf())!!.contains("lock"))
                ) {
                    logText("Where not selected, using default 'HomeScreen'")
                    sharedPreferences!!.edit().putStringSet("where", hashSetOf("home")).apply()
                }
                Functions.SetWallpaper(bitmap, blurred).execute()
//                img?.setImageBitmap(bitmap)
                val scaleBy = width
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaleBy, scaleBy, false)
                img?.setImageBitmap(scaledBitmap)
                latestTextView!!.text = sharedPreferences!!.getString("latest", "")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                logText("${e.javaClass.name}")
            }
        }
    }
}