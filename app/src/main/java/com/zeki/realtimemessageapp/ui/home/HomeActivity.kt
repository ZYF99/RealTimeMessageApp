package com.zeki.realtimemessageapp.ui.home

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.github.nkzawa.socketio.client.IO
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zeki.realtimemessageapp.databinding.ActivityMainBinding
import com.zeki.realtimemessageapp.model.RTCUser
import com.zeki.realtimemessageapp.ui.callvideopage.CallVideoActivity
import com.zeki.realtimemessageapp.utils.jsonToList
import com.zeki.realtimemessageapp.webrtc.Peer
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException

class HomeActivity : Activity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var isInit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()

    }

    private fun initView() {
        binding.rfIds.setOnRefreshListener {
            Peer.socket?.refreshIdList()
        }
        //配置在线用户列表
        binding.rvRtcUser.adapter = RTCRecyclerAdapter {
            //点击Call 开始视频通话
            RxPermissions(this).request(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).doOnNext { granted ->
                if (granted) {
                    CallVideoActivity.jumpHere(
                        context = this,
                        isCallComing = false,
                        toClientId = it.id
                    )
                }
            }.subscribe()
        }
    }

    private fun initSocket() {
        // 初始化 Socket 通信
        try {
            Peer.socket = IO.socket("http://192.168.58.14:3000")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        //连接成功后，服务端返回id
        Peer.socket?.on("id") { args ->
            val id = args[0] as String
            Log.d("id", id)
            runOnUiThread { Toast.makeText(this, "已连上服务器", Toast.LENGTH_SHORT).show() }
            //startLocalCamera( this@MainActivity)
            Peer.socket?.readyToStream(Build.MODEL)
        }

        //发送readyToStream后，服务器返回所有在线id列表
        Peer.socket?.on("ids") { args ->
            val jsonArray = args[0] as JSONArray
            Log.d("list", jsonArray.toString())
            runOnUiThread {
                Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
                val list: List<RTCUser> = jsonArray.toString().jsonToList() ?: listOf()
                (binding.rvRtcUser.adapter as RTCRecyclerAdapter).replaceData(list.filterNot { it.name == Build.MODEL })
                binding.rfIds.isRefreshing = false
            }
        }

        //来自对端的消息
        Peer.socket?.on("message") { args ->
            val data = args[0] as JSONObject
            try {
                val from = data.getString("from")
                val type = data.getString("type")

                //根据不同的指令类型和数据响应相应步骤的方法
                when (type) {
                    "call" -> receiveCall(from) //对面来电，写死直接接听电话
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        Peer.socket?.connect()
    }

    override fun onResume() {
        super.onResume()
            initSocket()
    }

    //接受对面的电话
    private fun receiveCall(fromClientId: String) {
        CallVideoActivity.jumpHere(context = this, isCallComing = true, fromClientId = fromClientId)
    }

}