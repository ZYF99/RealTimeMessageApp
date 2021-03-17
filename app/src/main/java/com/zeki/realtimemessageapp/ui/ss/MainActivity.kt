package com.zeki.realtimemessageapp.ui.ss

import android.Manifest
import android.widget.Toast
import com.zeki.realtimemessageapp.webrtc.WebRtcClient
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zeki.realtimemessageapp.webrtc.WebRtcApplication
import com.zeki.realtimemessageapp.webrtc.RtcActivity
import com.zeki.realtimemessageapp.ui.callvideopage.CallVideoActivity
import org.json.JSONArray
import org.webrtc.MediaStream

class MainActivity : RtcActivity() {



    override val rtcObserver: WebRtcClient.RtcListener = object : WebRtcClient.RtcListener {

        override fun onOnlineIdsChanged(jsonArray: JSONArray) {

        }

        override fun onCallReady(callId: String) {

        }

        override fun onStatusChanged(newStatus: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, newStatus, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onLocalStream(localStream: MediaStream) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "本地开启", Toast.LENGTH_SHORT).show()
            }

        }

        override fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "远程加入", Toast.LENGTH_SHORT).show()
            }
            CallVideoActivity.jumpHere(context = this@MainActivity)
        }

        override fun onRemoveRemoteStream(endPoint: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "有人退出视频${endPoint}", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun initView() {

        super.initView()

        setContentView(binding.root)

        //下拉刷新
        binding.rfIds.setOnRefreshListener {
            webRtcClient?.refreshIds()
        }

        //配置在线用户列表
        binding.rvRtcUser.adapter = RTCRecyclerAdapter {
            //点击Call 开始视频通话
            RxPermissions(this).request(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).doOnNext { granted ->
                if (granted) {
                    //webRtcClient?.onDestroy()
                    webRtcClient?.callByClientId(it.id)
                }
            }.subscribe()
        }
    }

    override fun initData() {
        super.initData()
        WebRtcApplication.instance?.initWebRtc()
    }

}