package com.rcs.htmlcrawlerdemo

import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.util.*

class HtmlClient {

    private val cacheDir = System.getProperty("user.dir") + "/data/"

    @Throws(IOException::class)
    fun getAsString(url: String): String {
        readFromCache(encodeToFilename(url))
            ?.let {
                println("Found URL in cache: $url")
                return it
            }

        println("Fetching URL: $url")

        val con = URI(url).toURL().openConnection() as HttpURLConnection
        con.requestMethod = "GET"

        val content = inputStreamToString(InputStreamReader(con.inputStream))

        writeToCache(encodeToFilename(url), content)

        return content
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

    private fun readFromCache(filename: String): String? {
        return try {
            inputStreamToString(FileReader(cacheDir + filename))
        } catch (e: FileNotFoundException) {
            null
        }
    }

    private fun writeToCache(filename: String, content: String) {
        FileWriter(cacheDir + filename)
            .use { writer -> writer.write(content) }
    }

    private fun encodeToFilename(url: String): String {
        return Base64.getUrlEncoder().encodeToString(url.toByteArray())
    }
}