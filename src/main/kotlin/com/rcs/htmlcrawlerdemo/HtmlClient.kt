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

        val content = StringBuilder()
        
        BufferedReader(InputStreamReader(con.inputStream))
            .use { reader ->
                var inputLine: String?
                while ((reader.readLine().also { inputLine = it }) != null) {
                    content.append(inputLine)
                }
            }

        val stringContent = content.toString()

        writeToFile(encodeToFilename(url), stringContent)

        return stringContent
    }

    private fun readFromCache(filename: String): String? {
        val content = java.lang.StringBuilder()

        val file: FileReader
        try {
            file = FileReader(cacheDir + filename)
        } catch (e: FileNotFoundException) {
            return null
        }

        BufferedReader(file)
            .use { reader ->
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    content.append(line).append(System.lineSeparator())
                }
            }

        return content.toString()
    }

    private fun writeToFile(filename: String, content: String) {
        FileWriter(cacheDir + filename)
            .use { writer -> writer.write(content) }
    }

    private fun encodeToFilename(url: String): String {
        return Base64.getUrlEncoder().encodeToString(url.toByteArray())
    }
}