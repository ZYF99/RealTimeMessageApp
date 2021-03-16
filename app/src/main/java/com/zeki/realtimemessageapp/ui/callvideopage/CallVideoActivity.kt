package com.zeki.realtimemessageapp.ui.callvideopage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.zeki.realtimemessageapp.databinding.ActivityCallVideoBinding
import com.zeki.realtimemessageapp.ui.base.RtcActivity
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
        override fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int) {}
        override fun onRemoveRemoteStream(endPoint: Int) {}
    }

    override fun initView() {

        super.initView()

        setContentView(binding.root)

        localRenderer = binding.localRenderer
        remoteRenderer = binding.remoteRenderer

        binding.btnCancel.setOnClickListener {

        }


    }

    override fun initData() {

        super.initData()

    }

/*    override fun onDestroy() {
        remoteMediaStream?.dispose()
        localMediaStream?.dispose()
        remoteMediaStream = null
        localMediaStream = null
        super.onDestroy()
    }*/

    companion object {
        fun jumpHere(context: Context, bundle: Bundle? = null) {
            val intent = Intent(context, CallVideoActivity::class.java).apply {
                if (bundle != null) putExtras(bundle)
            }
            context.startActivity(intent)
        }
    }

}