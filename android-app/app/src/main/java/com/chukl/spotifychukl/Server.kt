package com.chukl.spotifychukl

import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

/** Tiny local web server. Only this phone can reach it (127.0.0.1). */
object Server {
    const val PORT = 8765
    @Volatile private var socket: ServerSocket? = null

    fun start() {
        if (socket != null) return
        thread(isDaemon = true, name = "chukl-server") {
            try {
                val s = ServerSocket(PORT, 8, InetAddress.getByName("127.0.0.1"))
                socket = s
                while (!s.isClosed) {
                    val client = s.accept()
                    thread(isDaemon = true) {
                        client.use {
                            try {
                                it.getInputStream().bufferedReader().readLine()
                                val body = NowPlaying.toJson().toByteArray()
                                val head = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n" +
                                    "Access-Control-Allow-Origin: *\r\nContent-Length: ${body.size}\r\n" +
                                    "Connection: close\r\n\r\n"
                                it.getOutputStream().apply { write(head.toByteArray()); write(body); flush() }
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                socket = null
            }
        }
    }
}
