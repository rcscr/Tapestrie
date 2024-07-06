package com.rcs.htmlcrawlerdemo

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    fun getAsString(url: String): String = runBlocking {
        readFromCache(url)
            ?: fetch(url).also { writeToCache(url, it) }
    }

    private fun readFromCache(url: String): String? {
        return try {
            val fileReader = FileReader(cacheDirPath + encodeToFilename(url))
            val content = inputStreamToString(fileReader)
            fileReader.close()
            println("Found URL in cache: $url")
            content
        } catch (e: FileNotFoundException) {
            null
        }
    }

    @Throws(IOException::class)
    private fun fetch(url: String): String {
        println("Fetching URL: $url")
        var attempts = 0
        var result: String? = null
        var con: HttpURLConnection? = null

        while (attempts < 10) {
            try {
                con = URI(url).toURL().openConnection() as HttpURLConnection
                con.requestMethod = "GET"
                result = inputStreamToString(InputStreamReader(con.inputStream))
                break // Successful fetch, exit the loop
            } catch (e: Exception) {
                attempts++
                println("Attempt $attempts fetching $url failed: ${e.message}")
                Thread.sleep(500)
            } finally {
                con?.disconnect()             }
        }

        if (result == null) {
            throw IOException("Failed to fetch URL after 10 attempts: $url")
        }

        return result
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
        reader.close()
        return content.toString()
    }

    private suspend fun writeToCache(url: String, content: String): Unit = coroutineScope {
        launch {
            FileWriter(cacheDirPath + encodeToFilename(url))
                .use { writer -> writer.write(content) }
        }
    }

    private fun encodeToFilename(url: String): String {
        return Base64.getUrlEncoder().encodeToString(url.toByteArray())
    }
}