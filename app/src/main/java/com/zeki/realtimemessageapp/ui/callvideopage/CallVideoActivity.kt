package com.zeki.realtimemessageapp.ui.callvideopage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.zeki.realtimemessageapp.databinding.ActivityCallVideoBinding
import com.zeki.realtimemessageapp.webrtc.RtcActivity
import com.zeki.realtimemessageapp.webrtc.WebRtcClient
import org.webrtc.MediaStream

class CallVideoActivity : RtcActivity() {

    private val binding by lazy {
        ActivityCallVideoBinding.inflate(layoutInflater)
    }
    override val rtcObserver: WebRtcClient.RtcListener = object : WebRtcClient.RtcListener {
        override fun onCallReady(callId: String) {}
        override fun onStatusChanged(newStatus: String) {}
        override fun onLocalStream(localStream: MediaStream) {}
        override fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int) {
            //渲染远端画面
            remoteRenderer?.let { remoteStream.videoTracks?.get(0)?.addSink(it) }
        }

        override fun onRemoveRemoteStream(endPoint: Int) {
            finish()
        }
    }

    override fun initView() {

        super.initView()

        setContentView(binding.root)

        //渲染本地画面
        localRenderer?.let { localMediaStream?.videoTracks?.get(0)?.addSink(it) }

        //断开
        binding.btnCancel.setOnClickListener {
            finish()
        }

    }

    override fun onResume() {
        super.onResume()
        webRtcClient?.onResume()
    }

    override fun onPause() {
        super.onPause()
        webRtcClient?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        webRtcClient?.onDestroy()
    }

    companion object {
        fun jumpHere(
            context: Context,
            bundle: Bundle? = null
        ) {
            val intent = Intent(context, CallVideoActivity::class.java).apply {
                if (bundle != null) putExtras(bundle)
            }
            context.startActivity(intent)
        }
    }

}