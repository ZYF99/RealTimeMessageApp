package com.zeki.realtimemessageapp.ui.home

import com.github.nkzawa.socketio.client.Socket
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws

var socket: Socket? = null

//告诉服务端我在线了
fun Socket.readyToStream(name: String) {
    try {
        val message = JSONObject()
        message.put("name", name)
        emit("readyToStream", message)
    } catch (e: JSONException) {
        e.printStackTrace()
    }
}

//刷新id列表
fun Socket.refreshIdList(){
    emit("refreshids", null)
}

/**
 * Send a message through the signaling server
 *
 * @param to      id of recipient
 * @param type    type of message
 * @param payload payload of message
 * @throws JSONException
 */
@Throws(JSONException::class)
fun Socket.sendMessage(to: String, type: String, payload: JSONObject) {
    val message = JSONObject()
    message.put("to", to)
    message.put("type", type)
    message.put("payload", payload)
    emit("message", message)
}