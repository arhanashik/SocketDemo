package com.workfort.socketdemo

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException


class MainActivity : AppCompatActivity() {

    private var mSocket: Socket? = null
    interface Event {
        companion object {
            const val ADD_USER = "add user"
            const val NEW_MESSAGE = "new message"
            const val USER_JOINED = "user joined"
            const val USER_LEFT = "user left"
        }
    }

    init {
        try {
            mSocket = IO.socket("https://socket-io-chat.now.sh/")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSocket?.on(Socket.EVENT_CONNECT, onConnect)
        mSocket?.on(Socket.EVENT_DISCONNECT, onDisconnect)
        mSocket?.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
        mSocket?.on(Event.NEW_MESSAGE, onNewMessage)
        mSocket?.on(Event.USER_JOINED, onUserJoined)
        mSocket?.on(Event.USER_LEFT, onUserLeft)
        mSocket?.connect()
    }

    fun sendMessage(view: View) {
        val message = et_message.text.toString()
        if(TextUtils.isEmpty(message)) return

        et_message.setText("")

        if(username == null) {
            username = message
            mSocket?.emit(Event.ADD_USER, message)
            addMessage("system", "logged in as `$username`")
            et_message.setHint(R.string.hint_message)
            return
        }

        mSocket?.emit(Event.NEW_MESSAGE, message)
        addMessage("$username(me)", message)
    }

    private val onConnect = Emitter.Listener {
        runOnUiThread {
            addMessage("system", "Connected")
        }
    }

    private val onDisconnect = Emitter.Listener {
        runOnUiThread {
            addMessage("system", "Disconnected")
        }
    }

    private val onConnectError = Emitter.Listener {
        runOnUiThread {
            addMessage("system", "Error connecting")
        }
    }

    private val onNewMessage = Emitter.Listener { args ->
        runOnUiThread(Runnable {
            val data = args[0] as JSONObject
            val username: String
            val message: String
            try {
                username = data.getString("username")
                message = data.getString("message")
            } catch (e: JSONException) {
                return@Runnable
            }

            // add the message to view
            addMessage(username, message)
        })
    }

    private val onUserJoined = Emitter.Listener { args ->
        runOnUiThread(Runnable {
            val data = args[0] as JSONObject
            val username: String
            val numUsers: Int
            try {
                username = data.getString("username")
                numUsers = data.getInt("numUsers")
            } catch (e: JSONException) {
                Log.e(MainActivity::class.java.simpleName, e.message)
                return@Runnable
            }

            addMessage("system", "`$username` joined. Total user: $numUsers")
        })
    }

    private val onUserLeft = Emitter.Listener { args ->
        runOnUiThread(Runnable {
            val data = args[0] as JSONObject
            val username: String
            val numUsers: Int
            try {
                username = data.getString("username")
                numUsers = data.getInt("numUsers")
            } catch (e: JSONException) {
                Log.e(MainActivity::class.java.simpleName, e.message)
                return@Runnable
            }

            addMessage("system", "`$username` left. Total user: $numUsers")
        })
    }


    private fun addMessage(username: String, message: String) {
        val newMsg = "<br><br><font color=\"green\">$username: </font>" +
                "<font color=\"black\">$message</font>"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tv_messages.append(Html.fromHtml(newMsg, Html.FROM_HTML_MODE_LEGACY))
        } else {
            tv_messages.append(Html.fromHtml(newMsg))
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mSocket?.disconnect()
        mSocket?.off(Socket.EVENT_CONNECT, onConnect)
        mSocket?.off(Socket.EVENT_DISCONNECT, onDisconnect)
        mSocket?.off(Socket.EVENT_CONNECT_ERROR, onConnectError)
        mSocket?.off(Event.NEW_MESSAGE, onNewMessage)
        mSocket?.off(Event.USER_JOINED, onUserJoined)
        mSocket?.off(Event.USER_LEFT, onUserLeft)
    }
}
