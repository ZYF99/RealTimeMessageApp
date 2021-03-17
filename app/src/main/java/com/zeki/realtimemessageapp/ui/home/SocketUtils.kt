package com.zeki.realtimemessageapp.ui.home

import com.github.nkzawa.socketio.client.Socket
import org.json.JSONException
import org.json.JSONObject

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