package com.zeki.realtimemessageapp.ui.home

import android.util.Log
import com.zeki.realtimemessageapp.webrtc.WebRtcClient
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*

class Peer(val id: String, val endPoint: Int) : SdpObserver, PeerConnection.Observer {

    val pc: PeerConnection?

    init {
        Log.d("Peer :", "new Peer: $id $endPoint")
        this.pc = factory.createPeerConnection(iceServers, pcConstraints, this)
        this.pc?.setAudioRecording(true)
        this.pc?.setAudioPlayout(true)
        /*localMS?.addTrack(audioTrack)
        pc?.addStream(localMS!!) *///, new MediaConstraints()
        webrtcListener.onStatusChanged("CONNECTING")
    }

    override fun onCreateSuccess(sdp: SessionDescription) {
        // TODO: modify sdp to use pcParams prefered codecs
        try {
            val payload = JSONObject()
            payload.put("type", sdp.type.canonicalForm())
            payload.put("sdp", sdp.description)
            sendMessage(id, sdp.type.canonicalForm(), payload)
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
        webrtcListener.onStatusChanged(iceConnectionState.name)
        Log.d(WebRtcClient.TAG, "onIceConnectionChange ${iceConnectionState.name}")
        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            removePeer(id)
        }
    }

    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {}

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

    override fun onIceConnectionReceivingChange(p0: Boolean) {
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
    }

    override fun onAddStream(mediaStream: MediaStream) {
        Log.d(WebRtcClient.TAG, "onAddStream " + mediaStream.id)
        // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
        webrtcListener.onAddRemoteStream(mediaStream, endPoint + 1)
    }

    override fun onRemoveStream(mediaStream: MediaStream) {
        Log.d(WebRtcClient.TAG, "onRemoveStream " + mediaStream.id)
        removePeer(id)
    }

    override fun onDataChannel(dataChannel: DataChannel) {}

    override fun onRenegotiationNeeded() {

    }
}