package com.zeki.realtimemessageapp.webrtc

import android.util.Log
import com.github.nkzawa.socketio.client.Socket
import com.zeki.realtimemessageapp.ui.callvideopage.localMS
import com.zeki.realtimemessageapp.ui.home.sendMessage
import com.zeki.realtimemessageapp.ui.home.socket
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import java.util.*

class Peer(
    val id: String,
    val endPoint: Int,
    factory: PeerConnectionFactory,
    private val webRtcListener: RtcListener
) : SdpObserver, PeerConnection.Observer {

    companion object {
        const val TAG = "PEER LOG: "
        const val MAX_PEER = 2
        val peers = HashMap<String, Peer>()
        val iceServers = LinkedList<PeerConnection.IceServer>()
        val pcConstraints = MediaConstraints()
        var endPoints = BooleanArray(MAX_PEER)


    }

    val pc: PeerConnection?

    init {
        Log.d("Peer :", "new Peer: $id $endPoint")
        this.pc = factory.createPeerConnection(iceServers, pcConstraints, this)
        this.pc?.setAudioRecording(true)
        this.pc?.setAudioPlayout(true)
        val audioSource = factory.createAudioSource(MediaConstraints())
        val audioTrack = factory.createAudioTrack("audiotrack", audioSource)
        localMS?.addTrack(audioTrack)
        pc?.addStream(localMS!!)
        webRtcListener.onStatusChanged("CONNECTING")
    }

    override fun onCreateSuccess(sdp: SessionDescription) {
        // TODO: modify sdp to use pcParams prefered codecs
        try {
            val payload = JSONObject()
            payload.put("type", sdp.type.canonicalForm())
            payload.put("sdp", sdp.description)
            socket?.sendMessage(id, sdp.type.canonicalForm(), payload)
            pc?.setLocalDescription(this@Peer, sdp)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onSetSuccess() {}

    override fun onCreateFailure(s: String) {}

    override fun onSetFailure(s: String) {}

    override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {}

    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
        webRtcListener.onStatusChanged(iceConnectionState.name)
        Log.d(TAG, "onIceConnectionChange ${iceConnectionState.name}")
        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            removePeer(id, webRtcListener)
        }
    }

    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {}

    override fun onIceCandidate(candidate: IceCandidate) {
        try {
            val payload = JSONObject()
            payload.put("label", candidate.sdpMLineIndex)
            payload.put("id", candidate.sdpMid)
            payload.put("candidate", candidate.sdp)
            socket?.sendMessage(id, "candidate", payload)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {

    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {

    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {

    }

    override fun onAddStream(mediaStream: MediaStream) {
        Log.d(TAG, "onAddStream " + mediaStream.id)
        // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
        webRtcListener.onAddRemoteStream(mediaStream, endPoint + 1)
    }

    override fun onRemoveStream(mediaStream: MediaStream) {
        Log.d(TAG, "onRemoveStream " + mediaStream.id)
        removePeer(id, webRtcListener)
    }

    override fun onDataChannel(dataChannel: DataChannel) {}

    override fun onRenegotiationNeeded() {

    }

}

fun addPeer(id: String, endPoint: Int, facotry: PeerConnectionFactory, webRtcListener: RtcListener): Peer {
    val peer = Peer(id, endPoint, facotry, webRtcListener)
    Peer.peers[id] = peer
    Peer.endPoints[endPoint] = true
    return peer
}

fun removePeer(id: String, webRtcListener: RtcListener) {
    val peer = Peer.peers[id]
    webRtcListener.onRemoveRemoteStream(peer!!.endPoint)
    peer.pc?.close()
    Peer.peers.remove(peer.id)
    Peer.endPoints[peer.endPoint] = false
}
