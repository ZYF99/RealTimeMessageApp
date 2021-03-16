package com.zeki.realtimemessageapp.webrtc

import android.view.View
import com.zeki.realtimemessageapp.R
import com.zeki.realtimemessageapp.ui.base.BaseActivity
import org.json.JSONArray
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer

abstract class RtcActivity : BaseActivity() {

    abstract val rtcObserver: WebRtcClient.RtcListener

    val webRtcClient
    get() = WebRtcApplication.instance?.webRtcClient

    private val eglBase
    get() = WebRtcApplication.instance?.eglBase

    var localRenderer: SurfaceViewRenderer? = null
    var remoteRenderer: SurfaceViewRenderer? = null

    override fun initView() {
        localRenderer = findViewById(R.id.local_renderer)
        remoteRenderer = findViewById(R.id.remote_renderer)
        findViewById<View>(R.id.btn_cancel)?.setOnClickListener {

        }
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

    protected var localMediaStream:MediaStream? = null
    protected var remoteMediaStream:MediaStream? = null

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
                localMediaStream = localStream
                localRenderer?.let {
                    //渲染本地画面
                    localStream.videoTracks?.get(0)?.addSink(it)
                }
                observer.onLocalStream(localStream)
            }

            override fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int) {
                remoteMediaStream = remoteStream
                remoteRenderer?.let {
                    //渲染远端画面
                    remoteStream.videoTracks?.get(0)?.addSink(it)
                }
                observer.onAddRemoteStream(remoteStream, endPoint)
            }

            override fun onRemoveRemoteStream(endPoint: Int) {
                remoteRenderer?.let {
                    //渲染远端画面
                    remoteMediaStream?.videoTracks?.get(0)?.removeSink(it)
                }
                observer.onRemoveRemoteStream(endPoint)
            }

        })
    }

}