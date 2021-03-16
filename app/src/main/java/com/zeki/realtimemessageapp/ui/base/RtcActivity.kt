package com.zeki.realtimemessageapp.ui.base

import com.zeki.realtimemessageapp.webrtc.WebRtcApplication
import com.zeki.realtimemessageapp.webrtc.WebRtcClient
import org.json.JSONArray
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer

abstract class RtcActivity : BaseActivity() {

    abstract val rtcObserver: WebRtcClient.RtcListener

    val webRtcClient = WebRtcApplication.instance?.webRtcClient
    val eglBase = WebRtcApplication.instance?.eglBase

    var localRenderer: SurfaceViewRenderer? = null
    var remoteRenderer: SurfaceViewRenderer? = null

    override fun initView() {

    }

    override fun initData() {

        registerRtcEventObserver(rtcObserver)

        if (localRenderer != null)
            localRenderer?.apply {
                setEnableHardwareScaler(true)
                setMirror(true)
                init(eglBase?.eglBaseContext, null)
            }
        if (remoteRenderer != null)
            remoteRenderer?.apply {
                setEnableHardwareScaler(true)
                setMirror(true)
                init(eglBase?.eglBaseContext, null)
            }

    }

    private fun registerRtcEventObserver(observer: WebRtcClient.RtcListener) {
        WebRtcApplication.instance?.rtcListener?.add(object : WebRtcClient.RtcListener {

            override fun onOnlineIdsChanged(jsonArray: JSONArray) {
                observer.onOnlineIdsChanged(jsonArray)
            }

            override fun onCallReady(callId: String) {
                observer.onCallReady(callId)
            }

            override fun onStatusChanged(newStatus: String) {
                observer.onStatusChanged(newStatus)
            }

            override fun onLocalStream(localStream: MediaStream) {
                localRenderer?.let {
                    //渲染本地画面
                    localStream.videoTracks?.get(0)?.addSink(it)
                }
                observer.onLocalStream(localStream)
            }

            override fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int) {
                remoteRenderer?.let {
                    //渲染远端画面
                    remoteStream.videoTracks?.get(0)?.addSink(it)
                }
                observer.onAddRemoteStream(remoteStream, endPoint)
            }

            override fun onRemoveRemoteStream(endPoint: Int) {
                observer.onRemoveRemoteStream(endPoint)
            }

        })
    }

}