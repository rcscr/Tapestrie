package com.rcs.htmlcrawlerdemo

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

class HtmlClient {

    @Throws(IOException::class)
    fun getAsString(url: String): String {
        val con = URI(url).toURL().openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        val inputStream = BufferedReader(InputStreamReader(con.inputStream))
        var inputLine: String?
        val content = StringBuilder()
        while ((inputStream.readLine().also { inputLine = it }) != null) {
            content.append(inputLine)
        }
        inputStream.close()
        return content.toString()
    }
}