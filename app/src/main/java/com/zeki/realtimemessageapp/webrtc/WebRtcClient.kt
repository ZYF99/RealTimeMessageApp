package com.zeki.realtimemessageapp.webrtc

import android.app.Application
import android.content.Context
import android.util.Log
import com.github.nkzawa.emitter.Emitter
import com.zeki.realtimemessageapp.ui.home.sendMessage
import com.zeki.realtimemessageapp.ui.home.socket
import io.reactivex.schedulers.Schedulers
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import java.util.*
import kotlin.jvm.Throws
import org.webrtc.MediaConstraints
import org.webrtc.MediaConstraints.KeyValuePair


class WebRtcClient(
    private val app: Application,
    private val eglContext: EglBase.Context,
    private val webrtcListener: RtcListener
) {

    private val endPoints = BooleanArray(MAX_PEER)
    private val factory: PeerConnectionFactory
    val peers = HashMap<String, Peer>()
    private val iceServers = LinkedList<PeerConnection.IceServer>()
    private var localMediaStream: MediaStream? = null
    private val pcConstraints = MediaConstraints()
    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private val vc by lazy { getVideoCapture() }

    init {
        //初始化 PeerConnectionFactory 配置
        PeerConnectionFactory.initialize(
            PeerConnectionFactory
                .InitializationOptions
                .builder(app)
                .createInitializationOptions()
        )

        //初始化视频编码/解码信息
        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(
                DefaultVideoDecoderFactory(eglContext)
            )
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglContext, true, true
                )
            )
            .createPeerConnectionFactory()


        //初始化 ICE 服务器创建 PC 时使用
        iceServers.add(PeerConnection.IceServer("stun:23.21.150.121"))
        iceServers.add(PeerConnection.IceServer("stun:stun.l.google.com:19302"))

        //初始化本地的 MediaConstraints 创建 PC 时使用，是流媒体的配置信息
        pcConstraints.mandatory.add(KeyValuePair("OfferToReceiveAudio", "true"))

        pcConstraints.mandatory.add(KeyValuePair("maxHeight", 1920.toString()))
        pcConstraints.mandatory.add(KeyValuePair("maxWidth", 1080.toString()))
        pcConstraints.mandatory.add(KeyValuePair("maxFrameRate", 60.toString()))
        pcConstraints.mandatory.add(KeyValuePair("minFrameRate", 30.toString()))

        pcConstraints.mandatory.add(KeyValuePair("OfferToReceiveVideo", "true"))
        pcConstraints.optional.add(KeyValuePair("DtlsSrtpKeyAgreement", "true"))

    }

    // 初始化 Socket 通信
    fun initSocketMessage() {
        socket?.off()
        val messageHandler = MessageHandler()
        socket?.on("message", messageHandler.onMessage)
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
    private fun sendMessage(to: String, type: String, payload: JSONObject) {
        val message = JSONObject()
        message.put("to", to)
        message.put("type", type)
        message.put("payload", payload)
        socket?.emit("message", message)
    }

    private inner class MessageHandler {

        val onMessage = Emitter.Listener { args ->
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
                    if (endPoint == MAX_PEER) return@Listener
                    else addPeer(from, endPoint)
                }

                //根据不同的指令类型和数据响应相应步骤的方法
                when (type) {
                    "receive" -> initRtcCallChannel(from)//对面接受了我们的电话，开始建立WebRtc通话
                    //"leave" -> webrtcListener.onOtherLeave()//对面挂断了我们的电话
                    "init" -> createOffer(from)
                    "offer" -> createAnswer(from, payload)
                    "answer" -> setRemoteSdp(from, payload)
                    "candidate" -> addIceCandidate(from, payload)
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    //开始建立webRtc通话流程
    private fun initRtcCallChannel(clientId: String) {
        socket?.sendMessage(clientId, "init", JSONObject())
    }

    private fun createOffer(peerId: String) {
        Log.d(TAG, "CreateOfferCommand")
        val peer = peers[peerId]
        peer?.pc?.createOffer(peer, pcConstraints)
    }

    private fun createAnswer(peerId: String, payload: JSONObject?) {
        Log.d(TAG, "CreateAnswerCommand")
        val peer = peers[peerId]
        val sdp = SessionDescription(
            SessionDescription.Type.fromCanonicalForm(payload?.getString("type")),
            payload?.getString("sdp")
        )
        peer?.pc?.setRemoteDescription(peer, sdp)
        peer?.pc?.createAnswer(peer, pcConstraints)
    }

    private fun setRemoteSdp(peerId: String, payload: JSONObject?) {
        Log.d(TAG, "SetRemoteSDPCommand")
        val peer = peers[peerId]
        val sdp = SessionDescription(
            SessionDescription.Type.fromCanonicalForm(payload?.getString("type")),
            payload?.getString("sdp")
        )
        peer?.pc?.setRemoteDescription(peer, sdp)
    }

    private fun addIceCandidate(peerId: String, payload: JSONObject?) {
        Log.d(TAG, "AddIceCandidateCommand")
        val pc = peers[peerId]!!.pc
        if (pc!!.remoteDescription != null) {
            val candidate = IceCandidate(
                payload!!.getString("id"),
                payload.getInt("label"),
                payload.getString("candidate")
            )
            pc.addIceCandidate(candidate)
        }
    }

/*
* new Peer: fmCfhJQTZsWL7L0gAAAA 0
2021-03-24 15:04:35.021 26667-26895/com.zeki.realtimemessageapp D/com.zeki.realtimemessageapp.webrtc.WebRtcClient: CreateOfferCommand
2021-03-24 15:04:44.108 26667-26939/com.zeki.realtimemessageapp D/com.zeki.realtimemessageapp.webrtc.WebRtcClient: SetRemoteSDPCommand
2021-03-24 15:04:45.708 26667-26850/com.zeki.realtimemessageapp D/com.zeki.realtimemessageapp.webrtc.WebRtcClient: onIceConnectionChange CHECKING
2021-03-24 15:04:47.027 26667-26850/com.zeki.realtimemessageapp D/com.zeki.realtimemessageapp.webrtc.WebRtcClient: onAddStream LOCALMEDIASTREAM
2021-03-24 15:04:48.355 26667-26850/com.zeki.realtimemessageapp D/com.zeki.realtimemessageapp.webrtc.WebRtcClient: onIceConnectionChange CONNECTED
2021-03-24 15:05:26.464 26667-26850/com.zeki.realtimemessageapp D/com.zeki.realtimemessageapp.webrtc.WebRtcClient: onIceConnectionChange COMPLETED
2021-03-24 15:05:27.290 26667-26939/com.zeki.realtimemessageapp D/com.zeki.realtimemessageapp.webrtc.WebRtcClient: AddIceCandidateCommand
* */

    inner class Peer(val id: String, val endPoint: Int) : SdpObserver, PeerConnection.Observer {

        val pc: PeerConnection?

        init {
            Log.d(TAG, "new Peer: $id $endPoint")
            this.pc = factory.createPeerConnection(iceServers, pcConstraints, this)

            pc?.addStream(localMediaStream)

            //, new MediaConstraints()
            //webrtcListener.onStatusChanged("CONNECTING")
        }

        override fun onSetSuccess() {}

        override fun onCreateFailure(s: String) {
            Log.d(TAG, "onCreateFailure: $s")
        }

        override fun onSetFailure(s: String) {}

        override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {}

        override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {}

        override fun onIceConnectionReceivingChange(p0: Boolean) {
        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        }

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        }

        override fun onDataChannel(dataChannel: DataChannel) {}

        override fun onRenegotiationNeeded() {

        }

        // SDP 创建成功后回调，发送给服务器。
        override fun onCreateSuccess(sdp: SessionDescription) {
            // TODO: modify sdp to use pcParams prefered codecs
            try {
                val payload = JSONObject()
                payload.put("type", sdp.type.canonicalForm())
                payload.put("sdp", sdp.description)
                sendMessage(id, sdp.type.canonicalForm(), payload)
                pc!!.setLocalDescription(this@Peer, sdp)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        // ICE 框架获取候选者成功后的回调，发送给服务器。
        override fun onIceCandidate(candidate: IceCandidate) {
            try {
                val payload = JSONObject()
                payload.put("label", candidate.sdpMLineIndex)
                payload.put("id", candidate.sdpMid)
                payload.put("candidate", candidate.sdp)
                sendMessage(id, "candidate", payload)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }

        // ICE 连接状态变化时的回调
        override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
            //webrtcListener.onStatusChanged(iceConnectionState.name)
            Log.d(TAG, "onIceConnectionChange ${iceConnectionState.name}")
            if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                removePeer(id)
            }
        }

        //连接成功后，最后获取到媒体流，发给 View 层进行视频/音频的播放。
        override fun onAddStream(mediaStream: MediaStream) {
            Log.d(TAG, "onAddStream " + mediaStream.id)
            // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
            webrtcListener.onAddRemoteStream(mediaStream, endPoint + 1)

        }

        //媒体流断开
        override fun onRemoveStream(mediaStream: MediaStream) {
            Log.d(TAG, "onRemoveStream " + mediaStream.id)
            //removePeer(id)
        }

    }

    private fun addPeer(id: String, endPoint: Int) {
        val peer = Peer(id, endPoint)
        peers[id] = peer
        endPoints[endPoint] = true
    }

    @Synchronized
    private fun removePeer(id: String) {
        val peer = peers[id]
        endPoints[peer!!.endPoint] = false
        //peers.remove(peer.id)
        webrtcListener.onRemoveRemoteStream(peer.endPoint)
    }

    /**
     * Call this method in Activity.onPause()
     */
    fun onPause() {
        localVideoSource?.capturerObserver?.onCapturerStopped()
    }

    /**
     * Call this method in Activity.onResume()
     */
    fun onResume() {
        localVideoSource?.capturerObserver?.onCapturerStarted(true)
    }

    /**
     * Call this method in Activity.onDestroy()
     */
    fun onDestroy(isSender: Boolean) {
        try {
            if (isSender) {
                for (peer in peers.values) {
                    peer.pc?.dispose()
                }
                localMediaStream = null
                peers.clear()
            } else {
                Schedulers.newThread().scheduleDirect {
                    for (peer in peers.values) {
                        peer.pc?.dispose()
                    }
                    localMediaStream = null
                    peers.clear()
                }
            }
            vc.stopCapture()
            vc.dispose()
            /*socket?.disconnect()
            socket?.close()*/
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        socket?.off()
        /*socket?.disconnect()
        socket?.close()*/
    }

    private fun findEndPoint(): Int {
        for (i in 0 until MAX_PEER)
            if (!endPoints[i]) return i
        return MAX_PEER
    }

    /**
     * Start the socket.
     *
     *
     * Set up the local stream and notify the signaling server.
     * Call this method after onCallReady.
     *
     * @param name socket name
     */

    private fun getVideoCapture(): CameraVideoCapturer = getFrontVideoCapture(app)

    //开启本地摄像头
    fun startLocalCamera(context: Context) {

        //当前时间戳，用来标记流的名字，使流id唯一
        val t = System.currentTimeMillis()

        //初始化视频流
        localMediaStream = factory.createLocalMediaStream("LM$t")

        //视频源
        localVideoSource = factory.createVideoSource(false)

        //初始化图像采集工具
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglContext)
        (vc as VideoCapturer).initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)

        //开始采集图像
        vc.startCapture(320, 240, 60)

        //添加视频源到流
        localMediaStream?.addTrack(factory.createVideoTrack("LM$t", localVideoSource))

        //声音源
        localAudioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("audiotrack", localAudioSource)

        //添加声音源到流
        localMediaStream?.addTrack(localAudioTrack)

        //本地视频渲染回调
        webrtcListener.onLocalStream(localMediaStream!!)
    }

    //通过id 打对面电话
    fun callByClientId(clientId: String) {
        socket?.sendMessage(clientId, "call", JSONObject())
        //Peer.socket?.sendMessage(clientId, "init", JSONObject())
    }

    //通过id 接对面电话
    fun receiveByClientId(clientId: String) {
        socket?.sendMessage(clientId, "receive", JSONObject())
        //Peer.socket?.sendMessage(clientId, "init", JSONObject())
    }

    //告诉服务器自己离开了
    fun leaveByClientId() {
        socket?.emit("leave", JSONObject())
        //Peer.socket?.sendMessage(clientId, "init", JSONObject())
    }

    /**
     * Implement this interface to be notified of events.
     */
    interface RtcListener {

        fun onLocalStream(localStream: MediaStream)

        fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int)

        fun onRemoveRemoteStream(endPoint: Int)
    }

    companion object {
        private val TAG = WebRtcClient::class.java.canonicalName
        private const val MAX_PEER = 2
    }
}