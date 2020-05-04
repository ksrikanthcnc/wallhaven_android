package com.wallhaven

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import com.wallhaven.Functions.Companion.dev
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

const val APIString: String = "https://wallhaven.cc/api/v1/search?"
var isSetting: Boolean = false
val mutex: Semaphore = Semaphore(1, true)

class API {
    fun getSet(jsonURL: JSONObject) {
        AGetSet(jsonURL).execute()
    }

    private fun Boolean.toInt() = if (this) '1' else '0'
    fun getURL(): JSONObject {
        val json = JSONObject()

        val userSearch = sharedPreferences!!.getString("search", "")!!
        var search = ""
        if (userSearch.isNotBlank()) {
            search = "&q=$userSearch"
        }

        val categoriesSet = sharedPreferences!!.getStringSet("categories", hashSetOf())!!
        if (categoriesSet.isEmpty()) {
            sharedPreferences!!.edit().putStringSet("categories", hashSetOf("general")).apply()
            categoriesSet.add("general")
        }
        val general = categoriesSet.contains("general")
        val anime = categoriesSet.contains("anime")
        val people = categoriesSet.contains("people")
        val categories = "&categories=" + general.toInt() + anime.toInt() + people.toInt()

        val puritySet = sharedPreferences!!.getStringSet("purity", hashSetOf())!!
        if (puritySet.isEmpty()) {
            sharedPreferences!!.edit().putStringSet("purity", hashSetOf("sfw")).apply()
            puritySet.add("sfw")
        }
        var sfw = puritySet.contains("sfw")
        val sketchy = puritySet.contains("sketchy")
        val nsfw = puritySet.contains("nsfw")
        /* API issue, else fetching &purity=111 */
        if (!dev)
            if (!(sfw || sketchy || nsfw))
                sfw = true
        val purity = "&purity=" + sfw.toInt() + sketchy.toInt() + nsfw.toInt()

        val sortingSet = sharedPreferences!!.getStringSet("sorting", hashSetOf())!!
        if (sortingSet.isEmpty()) {
            sharedPreferences!!.edit().putStringSet("sorting", hashSetOf("general")).apply()
            sortingSet.add("date_added")
        }
        //todo remove default
        var sorting = "&sorting=random"
        if (sortingSet.isNotEmpty()) {
            val randomSorter = sortingSet.shuffled().first()
            sorting = "&sorting=$randomSorter"
        }

        var userOrder = sharedPreferences!!.getString("order", "")!!
        if (userOrder.isBlank()) {
            sharedPreferences!!.edit().putString("order", "desc").apply()
            userOrder = "desc"
        }
        var order = ""
        if (userOrder.isNotEmpty()) {
            order = "&order=$userOrder"
        }

        var userTopRange = sharedPreferences!!.getString("toprange", "")!!
        if (userTopRange.isBlank()) {
            sharedPreferences!!.edit().putString("toprange", "1M").apply()
            userTopRange = "1M"
        }
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

        val page: String
        val sorter: String = if (sorting == "toplist")
            "$sorting${sharedPreferences!!.getString("toprange", "1M")}"
        else
            sorting

        val currentPage = sharedPreferences!!.getString("page", "1")
        page = "&page=$currentPage"

        val userIndex = sharedPreferences!!.getInt("index", 1)
        var index = userIndex

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
            var urls = sharedPreferences!!.getStringSet("urls", hashSetOf())!!
            url = jsonToString(jsonURL)
            if (urls.isEmpty()) {
                var msg = ""
                val json = JSONObject(fetchJSONString(url)!!)
                val meta = json.get("meta") as JSONObject
                if (meta.get("total") == 0) {
                    msg = "No images with given params"
                    sharedPreferences!!.edit().putBoolean("run", false).apply()
                } else {
                    msg = "Refreshing cache list"
                    updatePage(json, jsonURL)
                    val newUrl = jsonToString(jsonURL)
                    val newJson = JSONObject(fetchJSONString(newUrl)!!)
                    val newUrls = dataToUrls(newJson.get("data") as JSONArray)
                    sharedPreferences!!.edit().putStringSet("urls", newUrls).apply()
                    urls = newUrls
                }
                t(msg)
            }
            val url = getFirstURL(urls)
            val image: Bitmap? = getIMG(url)
            return image
        }

        private fun getFirstURL(urls: Set<String>): String {
            //todo don 't randomize
            var url = ""
            url = urls.first()
            sharedPreferences!!.edit().putString("latest", "${jsonToString(jsonURL)}\n[$url]").apply()
            sharedPreferences!!.edit().putStringSet("urls", urls.drop(1).toSet()).apply()
            return url
        }

        private fun updatePage(json: JSONObject, jsonURL: JSONObject) {
            val meta = json.get("meta") as JSONObject
            val currentPage = meta.get("current_page")
            var page = (currentPage as Int) + 1
            val lastPage = meta.get("last_page") as Int
            if (page > lastPage)
                page = 1
            jsonURL.put("page", "&page=$page")
            sharedPreferences!!.edit().putString("page", page.toString()).apply()
        }

        fun dataToUrls(data: JSONArray): Set<String> {
            val urls = ArrayList<String>()
            for (i in 0 until data.length()) {
                val obj = data.get(i) as JSONObject
                val path = obj.get("path") as String
                urls.add(path)
            }
            return urls.toSet()
        }

        override fun onPostExecute(image: Bitmap?) {
            super.onPostExecute(image)
            if (image != null)
                setBitmap(image)
            unsetting()
        }

        private fun jsonToString(jsonURL: JSONObject): String {
            return "${jsonURL.get("API")}${jsonURL.get("search")}${jsonURL.get("categories")}${jsonURL.get("purity")}${jsonURL.get("sorting")}" +
                    "${jsonURL.get("order")}" + "${jsonURL.get("toprange")}${jsonURL.get("atleast")}${jsonURL.get("resolutions")}${jsonURL.get("ratios")}" +
                    "${jsonURL.get("colors")}" + "${jsonURL.get("page")}${jsonURL.get("seed")}${jsonURL.get("apikey")}"
        }

        private fun fetchJSONString(URL: String): String? {
            return try {
                //todo throws error
                log += "[$URL]JSON"
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
            log += "[$uri]Downloading\n"
            val url = URL(uri)
            return try {
                BitmapFactory.decodeStream(url.openConnection().getInputStream())
            } catch (e: Exception) {
                err = e.javaClass.name
                log += err
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
                    log += "[Using default home]"
                    sharedPreferences!!.edit().putStringSet("where", hashSetOf("home")).apply()
                }
                MainActivity.setWallpaper(bitmap, blurred)
                img?.setImageBitmap(bitmap)
                latestTextView!!.text = sharedPreferences!!.getString("latest", "")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                log += e.javaClass.name + "\n"
            }
        }
    }
}