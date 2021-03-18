package com.zeki.realtimemessageapp.webrtc

import org.webrtc.MediaStream

interface RtcListener {

    fun onStatusChanged(newStatus: String)

    fun onLocalStream(localStream: MediaStream)

    fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int)

    fun onRemoveRemoteStream(endPoint: Int)
}
