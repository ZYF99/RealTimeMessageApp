package com.zeki.realtimemessageapp.webrtc

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.zeki.realtimemessageapp.utils.getFrontVideoCapture
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import java.net.URISyntaxException
import java.util.*
import kotlin.concurrent.timer

class WebRtcClient(
    url: String,
    private val app: Application,
    private val eglContext: EglBase.Context,
    private val webrtcListener: RtcListener
) {

    private val endPoints = BooleanArray(MAX_PEER)
    private val factory: PeerConnectionFactory


    private var localMS: MediaStream? = null
    private var videoSource: VideoSource? = null

    private val vc by lazy { getVideoCapture() }

    var audioTrack: AudioTrack? = null

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

        val audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack = factory.createAudioTrack("audiotrack", audioSource)


    }




    fun callByClientId(clientId: String) {
        sendMessage(clientId, "init", JSONObject())
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



    private fun createOffer(peerId: String) {
        Log.d(TAG, "CreateOfferCommand")
        val peer = peers[peerId]

        localMS?.addTrack(audioTrack)
        peer?.pc?.addStream(localMS!!)

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




    private fun addPeer(id: String, endPoint: Int): Peer {
        val peer = Peer(id, endPoint)
        peers[id] = peer

        endPoints[endPoint] = true
        return peer
    }

    private fun removePeer(id: String) {
        val peer = peers[id]
        webrtcListener.onRemoveRemoteStream(peer!!.endPoint)
        peer.pc!!.close()
        peers.remove(peer.id)
        endPoints[peer.endPoint] = false
    }

    /**
     * Call this method in Activity.onPause()
     */
    fun onPause() {
        videoSource?.capturerObserver?.onCapturerStopped()
    }

    /**
     * Call this method in Activity.onResume()
     */
    fun onResume() {
        videoSource?.capturerObserver?.onCapturerStarted(true)
    }

    /**
     * Call this method in Activity.onDestroy()
     */
    fun onDestroy() {
        for (peer in peers.values) {
            peer.pc?.dispose()
        }
        videoSource?.dispose()
        factory.dispose()
        socket?.disconnect()
        socket?.close()
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



    //开启本地视频流
    fun startLocalCamera(context: Context) {
        //init local media stream
        val localVideoSource = factory.createVideoSource(false)
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(
                Thread.currentThread().name, eglContext
            )
        (vc as VideoCapturer).initialize(
            surfaceTextureHelper,
            context,
            localVideoSource.capturerObserver
        )
        vc.startCapture(320, 240, 60)
        localMS = factory.createLocalMediaStream("LOCALMEDIASTREAM")
        localMS?.addTrack(factory.createVideoTrack("LOCALMEDIASTREAM", localVideoSource))
        webrtcListener.onLocalStream(localMS!!)
        //readyToStream(Build.MODEL)
    }

    /**
     * Implement this interface to be notified of events.
     */
    interface RtcListener {

        fun onStatusChanged(newStatus: String)

        fun onLocalStream(localStream: MediaStream)

        fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int)

        fun onRemoveRemoteStream(endPoint: Int)
    }

    companion object {
        private val TAG = WebRtcClient::class.java.canonicalName
        private const val MAX_PEER = 2
    }
}
