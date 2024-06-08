package com.rcs.htmlcrawlerdemo

import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.util.*

class HtmlClient {

    private val cacheDirPath = System.getProperty("user.dir") + "/data/"

    init {
        val cacheDir = File(cacheDirPath)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    @Throws(IOException::class)
    fun getAsString(url: String): String {
        return readFromCache(url)
            ?: fetch(url).also { writeToCache(url, it) }
    }

    private fun readFromCache(url: String): String? {
        return try {
            val content = inputStreamToString(FileReader(cacheDirPath + encodeToFilename(url)))
            println("Found URL in cache: $url")
            content
        } catch (e: FileNotFoundException) {
            null
        }
    }

    private fun fetch(url: String): String {
        println("Fetching URL: $url")
        val con = URI(url).toURL().openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        return inputStreamToString(InputStreamReader(con.inputStream))
    }

    private fun inputStreamToString(reader: Reader): String {
        val content = StringBuilder()
        BufferedReader(reader)
            .use {
                var line: String?
                while ((it.readLine().also { line = it }) != null) {
                    content.append(line).append(System.lineSeparator())
                }
            }
        return content.toString()
    }

    private fun writeToCache(url: String, content: String) {
        FileWriter(cacheDirPath + encodeToFilename(url))
            .use { writer -> writer.write(content) }
    }

    private fun encodeToFilename(url: String): String {
        return Base64.getUrlEncoder().encodeToString(url.toByteArray())
    }
}