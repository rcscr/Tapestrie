package com.rcs.htmlcrawlerdemo

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class HtmlClient {

    @Throws(IOException::class)
    fun getAsString(url: String): String {
        val con = URI(url).toURL().openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        val `in` = BufferedReader(InputStreamReader(con.inputStream))
        var inputLine: String?
        val content = StringBuilder()
        while ((`in`.readLine().also { inputLine = it }) != null) {
            content.append(inputLine)
        }
        `in`.close()
        return content.toString()
    }
}