package com.zeki.realtimemessageapp.webrtc

import android.content.Context
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.VideoCapturer

fun getFrontVideoCapture(app:Context):CameraVideoCapturer{
    return if (Camera2Enumerator.isSupported(app)) {
        Camera2Enumerator(app).let { camera ->
            camera.createCapturer(
                camera.deviceNames.find { cameraName -> camera.isFrontFacing(cameraName) } ?: camera.deviceNames[0],
                null
            )
        }
    } else {
        Camera1Enumerator(true).let { camera ->
            camera.createCapturer(
                camera.deviceNames.find { cameraName -> camera.isFrontFacing(cameraName) } ?: camera.deviceNames[0],
                null
            )
        }
    }
}

fun getBackVideoCapture(app:Context):VideoCapturer{
    return if (Camera2Enumerator.isSupported(app)) {
        Camera2Enumerator(app).let { camera ->
            camera.createCapturer(
                camera.deviceNames.find { cameraName -> camera.isBackFacing(cameraName) } ?: camera.deviceNames[0],
                null
            )
        }
    } else {
        Camera1Enumerator(true).let { camera ->
            camera.createCapturer(
                camera.deviceNames.find { cameraName -> camera.isBackFacing(cameraName) } ?: camera.deviceNames[0],
                null
            )
        }
    }
}