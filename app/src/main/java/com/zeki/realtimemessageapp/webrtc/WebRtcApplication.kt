package com.zeki.realtimemessageapp.webrtc

import android.app.Application
import org.json.JSONArray
import org.webrtc.EglBase
import org.webrtc.MediaStream

abstract class WebRtcApplication(val signalServerIpAddress: String) : Application() {

    companion object {
        var instance: WebRtcApplication? = null
    }

    //webRtc客户端
    var webRtcClient: WebRtcClient? = null

    //所有WebRTC事件的观察者
    val rtcListener: MutableList<WebRtcClient.RtcListener> = mutableListOf()

    val eglBase: EglBase by lazy { EglBase.create() }

    override fun onCreate() {
        super.onCreate()

        instance = this

        //initWebRtc()

    }

    fun initWebRtc() {
        webRtcClient = WebRtcClient(
            signalServerIpAddress, //信令服务器地址
            instance!!,
            eglBase.eglBaseContext,
            object : WebRtcClient.RtcListener {

                override fun onOnlineIdsChanged(jsonArray: JSONArray) {
                    rtcListener.forEach {
                        it.onOnlineIdsChanged(jsonArray)
                    }
                }

                override fun onCallReady(callId: String) {
                    rtcListener.forEach {
                        it.onCallReady(callId)
                    }
                }

                override fun onStatusChanged(newStatus: String) {
                    rtcListener.forEach {
                        it.onStatusChanged(newStatus)
                    }
                }

                override fun onLocalStream(localStream: MediaStream) {
                    rtcListener.forEach {
                        it.onLocalStream(localStream)
                    }
                }

                override fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int) {
                    rtcListener.forEach {
                        it.onAddRemoteStream(remoteStream, endPoint)
                    }
                }

                override fun onRemoveRemoteStream(endPoint: Int) {
                    rtcListener.forEach {
                        it.onRemoveRemoteStream(endPoint)
                    }
                }
            })
    }

}