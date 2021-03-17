package com.zeki.realtimemessageapp.ui.home

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.zeki.realtimemessageapp.databinding.ActivityMainBinding
import com.zeki.realtimemessageapp.model.RTCUser
import com.zeki.realtimemessageapp.ui.ss.RTCRecyclerAdapter
import com.zeki.realtimemessageapp.utils.jsonToList
import com.zeki.realtimemessageapp.webrtc.WebRtcClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import java.net.URISyntaxException
import java.util.*

class HomeActivity : Activity() {


    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val iceServers = LinkedList<PeerConnection.IceServer>()
    private val peers = HashMap<String, Peer>()
    private val pcConstraints = MediaConstraints()
    private var socket: Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
        initICEAndStreamOption()
        initSocket()

    }

    private fun initView() {
        binding.rfIds.setOnRefreshListener {
            socket?.refreshIdList()
        }
    }

    private fun initICEAndStreamOption() {
        //初始化 ICE 服务器创建 PC 时使用
        iceServers.add(PeerConnection.IceServer("stun:132.232.240.86:3478"))

        //初始化本地的 MediaConstraints 创建 PC 时使用，是流媒体的配置信息
        pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxHeight", "720"))
        pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxWidth", "1280"))
        pcConstraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
    }

    private fun initSocket() {
        // 初始化 Socket 通信
        try {
            socket = IO.socket("http://192.168.58.14:3000")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        //来自对端的消息
        socket?.on("message") { args ->
            val data = args[0] as JSONObject
            try {
                val from = data.getString("from")
                val type = data.getString("type")
                var payload: JSONObject? = null
                if (type != "init") {
                    payload = data.getJSONObject("payload")
                }
                //用于检查是否 PC 是否已存在已经是否达到最大的2个 PC 的限制
                if (!peers.containsKey(from)) {
                    val endPoint = findEndPoint()
                    if (endPoint == WebRtcClient.MAX_PEER) return@Listener
                    else addPeer(from, endPoint)
                }
                //根据不同的指令类型和数据响应相应步骤的方法
                when (type) {
                    "init" -> {
                        startLocalCamera(context = app)
                        createOffer(from)
                    }
                    "offer" -> createAnswer(from, payload)
                    "answer" -> setRemoteSdp(from, payload)
                    "candidate" -> addIceCandidate(from, payload)
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        //连接成功后，服务端返回id
        socket?.on("id") { args ->
            val id = args[0] as String
            Log.d("id", id)
            runOnUiThread { Toast.makeText(this, "已连上服务器", Toast.LENGTH_SHORT).show() }
            //startLocalCamera( this@MainActivity)
            socket?.readyToStream(Build.MODEL)
        }

        //发送readyToStream后，服务器返回所有在线id列表
        socket?.on("ids") { args ->
            val jsonArray = args[0] as JSONArray
            Log.d("list", jsonArray.toString())
            runOnUiThread {
                Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
                val list: List<RTCUser> = jsonArray.toString().jsonToList() ?: listOf()
                (binding.rvRtcUser.adapter as RTCRecyclerAdapter).replaceData(list)
                binding.rfIds.isRefreshing = false
            }
        }
        socket?.connect()
    }


}