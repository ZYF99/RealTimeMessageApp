package com.zeki.realtimemessageapp.ui.callvideopage

import android.app.Activity
import org.webrtc.*

const val KEY_TO_USER_ID = "key_rtc_to_user_id"
const val KEY_FROM_USER_ID = "key_rtc_from_user_id"
const val KEY_IS_CALL_COMING = "key_is_coming"

class CallVideoActivity : Activity() {

    /*private var factory: PeerConnectionFactory? = null
    private var videoSource: VideoSource? = null

    private val vc by lazy { getFrontVideoCapture(application) }
    var audioTrack: AudioTrack? = null
    val eglBase: EglBase by lazy { EglBase.create() }

    private var webRtcListener: RtcListener? = null

    private val binding by lazy { ActivityCallVideoBinding.inflate(layoutInflater) }

    private val isCallComing by lazy { intent.extras?.getBoolean(KEY_IS_CALL_COMING) }
    private val toId by lazy { intent.extras?.getString(KEY_TO_USER_ID) }
    private val fromId by lazy { intent.extras?.getString(KEY_FROM_USER_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        initWebRtcListener()
        initPeerFactory()
        initICEAndStreamOption()
        initSocketMessage()
        if (isCallComing == true) {
            RxPermissions(this).request(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).doOnNext { granted ->
                if (granted) {
                    startLocalCamera(context = applicationContext)
                    socket?.sendMessage(fromId!!, "receive", JSONObject())
                }
            }.subscribe()
        } else {
            startLocalCamera(context = applicationContext)
            callByClientId(toId!!)
        }
    }

    private fun initView() {

        binding.localRenderer.apply {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglBase.eglBaseContext, null)
        }

        binding.remoteRenderer.apply {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglBase.eglBaseContext, null)
        }

        //断开
        binding.btnCancel.setOnClickListener {
            leave(true)
        }

    }

    private fun initSocketMessage() {
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
                if (!Peer.peers.containsKey(from)) {
                    val endPoint = findEndPoint()
                    if (endPoint == Peer.MAX_PEER) return@on
                    else addPeer(from, endPoint, factory!!, webRtcListener!!)
                }
                //根据不同的指令类型和数据响应相应步骤的方法
                when (type) {
                    "receive" -> initRtcCallChannel(from)//对面接受了我们的电话，开始建立WebRtc通话
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

    //初始化Peer设置参数
    private fun initPeerFactory() {
        //初始化 PeerConnectionFactory 配置
        PeerConnectionFactory.initialize(
            PeerConnectionFactory
                .InitializationOptions
                .builder(applicationContext)
                .createInitializationOptions()
        )

        //初始化视频编码/解码信息
        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(
                DefaultVideoDecoderFactory(eglBase.eglBaseContext)
            )
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            )
            .createPeerConnectionFactory()

        val audioSource = factory?.createAudioSource(MediaConstraints())
        audioTrack = factory?.createAudioTrack("audiotrack", audioSource)
    }

    //端id
    private fun findEndPoint(): Int {
        for (i in 0 until Peer.MAX_PEER)
            if (!Peer.endPoints[i]) return i
        return Peer.MAX_PEER
    }

    private val surfaceTextureHelper: SurfaceTextureHelper by lazy {
        SurfaceTextureHelper.create(Thread.currentThread().name, eglBase.eglBaseContext)
    }

    //开启本地视频流
    private fun startLocalCamera(context: Context) {
        //init local media stream
        val localVideoSource = factory?.createVideoSource(false)
        (vc as VideoCapturer).initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
        vc.startCapture(320, 240, 60)
        localMS = factory?.createLocalMediaStream("LOCALMEDIASTREAM")
        localMS?.addTrack(factory?.createVideoTrack("LOCALMEDIASTREAM", localVideoSource))

        webRtcListener?.onLocalStream(localMS!!)
        //readyToStream(Build.MODEL)
    }

    private fun createOffer(peerId: String) {
        Log.d(TAG, "CreateOfferCommand")
        val peer = Peer.peers[peerId]
        peer?.pc?.createOffer(peer, Peer.pcConstraints)
    }

    private fun createAnswer(peerId: String, payload: JSONObject?) {
        Log.d(TAG, "CreateAnswerCommand")
        val peer = Peer.peers[peerId]
        val sdp = SessionDescription(
            SessionDescription.Type.fromCanonicalForm(payload?.getString("type")),
            payload?.getString("sdp")
        )
        peer?.pc?.setRemoteDescription(peer, sdp)
        peer?.pc?.createAnswer(peer, Peer.pcConstraints)
    }

    private fun setRemoteSdp(peerId: String, payload: JSONObject?) {
        Log.d(TAG, "SetRemoteSDPCommand")
        val peer = Peer.peers[peerId]
        val sdp = SessionDescription(
            SessionDescription.Type.fromCanonicalForm(payload?.getString("type")),
            payload?.getString("sdp")
        )
        peer?.pc?.setRemoteDescription(peer, sdp)
    }

    private fun addIceCandidate(peerId: String, payload: JSONObject?) {
        Log.d(TAG, "AddIceCandidateCommand")
        val pc = Peer.peers[peerId]!!.pc
        if (pc!!.remoteDescription != null) {
            val candidate = IceCandidate(
                payload!!.getString("id"),
                payload.getInt("label"),
                payload.getString("candidate")
            )
            pc.addIceCandidate(candidate)
        }
    }

    private fun initICEAndStreamOption() {
        //初始化 ICE 服务器创建 PC 时使用
        Peer.iceServers.add(PeerConnection.IceServer("stun:132.232.240.86:3478"))

        //初始化本地的 MediaConstraints 创建 PC 时使用，是流媒体的配置信息
        Peer.pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        Peer.pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        Peer.pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxHeight", "720"))
        Peer.pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxWidth", "1280"))
        Peer.pcConstraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
    }

    var localStreamCache: MediaStream? = null
    var remoteStreamCache: MediaStream? = null

    private fun initWebRtcListener() {
        webRtcListener = object : RtcListener {

            override fun onStatusChanged(newStatus: String) {
                runOnUiThread { Toast.makeText(this@CallVideoActivity, newStatus, Toast.LENGTH_SHORT).show() }
            }

            override fun onLocalStream(localStream: MediaStream) {
                localStreamCache = localStream
                localStream.videoTracks[0].addSink(binding.localRenderer)
            }

            override fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int) {
                remoteStreamCache = remoteStream
                remoteStream.videoTracks[0].addSink(binding.remoteRenderer)
            }

            override fun onRemoveRemoteStream(endPoint: Int) {
                leave(isActive = false)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        videoSource?.capturerObserver?.onCapturerStopped()
    }

    override fun onResume() {
        super.onResume()
        videoSource?.capturerObserver?.onCapturerStarted(true)
    }

    private fun leave(isActive: Boolean) {
        if (isActive) {
            for (peer in peers.values) {
                peer.pc?.dispose()
            }
        }
        peers.clear()
        iceServers.clear()
        endPoints = BooleanArray(Peer.MAX_PEER)
        Schedulers.io().scheduleDirect {
            videoSource?.dispose()
            //factory?.dispose()
            socket?.disconnect()
            socket?.close()
        }
        vc.dispose()
        finish()
    }

    companion object {
        private val TAG = HomeActivity::class.java.canonicalName

        fun jumpHere(
            context: Context,
            isCallComing: Boolean = false,
            toClientId: String? = null, //当isCallComing为false时必填
            fromClientId: String? = null //当isCallComing为true时必填
        ) {
            val intent = Intent(context, CallVideoActivity::class.java).apply {
                putExtra(KEY_IS_CALL_COMING, isCallComing)
                putExtra(KEY_FROM_USER_ID, fromClientId)
                putExtra(KEY_TO_USER_ID, toClientId)
            }
            context.startActivity(intent)
        }
    }
*/
}