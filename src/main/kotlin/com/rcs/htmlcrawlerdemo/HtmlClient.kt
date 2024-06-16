package com.rcs.htmlcrawlerdemo

import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.function.BiConsumer

class HtmlClient(private val executorService: ExecutorService) {

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

    fun forEachFileInCacheDir(consumer: BiConsumer<String, String>) {
        val directory = File(cacheDirPath)
        if (directory.isDirectory) {
            val files = directory.listFiles()!!
            files.forEach { file ->
                val reader = FileReader(file)
                consumer.accept(decodeFromFilename(file.name), inputStreamToString(reader))
                reader.close()
            }
        }
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

    private fun writeToCache(url: String, content: String) {
        executorService.submit {
            FileWriter(cacheDirPath + encodeToFilename(url))
                .use { writer -> writer.write(content) }
        }
    }

    private fun encodeToFilename(url: String): String {
        return Base64.getUrlEncoder().encodeToString(url.toByteArray())
    }

    private fun decodeFromFilename(filename: String): String {
        return String(Base64.getUrlDecoder().decode(filename))
    }
}