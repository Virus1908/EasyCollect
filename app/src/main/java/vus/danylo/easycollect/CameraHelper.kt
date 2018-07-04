package vus.danylo.easycollect

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import java.util.*
import android.graphics.Bitmap
import java.io.FileOutputStream
import java.io.IOException
import android.hardware.camera2.CameraAccessException
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat


private const val TAG: String = "CameraHelper"

class CameraHelper {

    private var textureView: TextureView? = null
    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private var cameraCaptureSession: CameraCaptureSession? = null

    private var imageDimension: Size? = null
    private lateinit var galleryFolder: File

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    fun setTextureView(texture: TextureView) {
        this.textureView = texture
        texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

        }
    }

    fun setCameraManager(cameraManager: CameraManager) {
        this.cameraManager = cameraManager
    }

    fun setGalleryFolder(galleryFolder: File) {
        this.galleryFolder = galleryFolder
    }

    fun resume() {
        startBackgroundThread()
        if (textureView != null && textureView?.isAvailable!!) {
            openCamera()
        }
    }

    fun pause() {
        stopBackgroundThread()
        closeCamera()
    }

    fun takePicture(context: Context) {
        lock()
        var outputPhoto: FileOutputStream? = null
        try {
            outputPhoto = FileOutputStream(createImageFile(galleryFolder))
            val textureBitmap = textureView!!.bitmap
            Bitmap.createBitmap(textureBitmap,
                    0, (textureBitmap.height - textureBitmap.width) / 2,
                    textureBitmap.width, textureBitmap.width)
                    .compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            unlock()
            try {
                if (outputPhoto != null) {
                    outputPhoto.close()
                    Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        if (cameraManager == null) {
            return
        }
        try {
            val cameraId = cameraManager!!.cameraIdList[0]
            val characteristics = cameraManager!!.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            imageDimension = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), textureView!!.width, textureView!!.height)
            cameraManager!!.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice?) {
                    cameraDevice = camera
                    createCameraPreview()
                }

                override fun onDisconnected(camera: CameraDevice?) {
                    cameraDevice?.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice?, error: Int) {
                    cameraDevice?.close()
                    cameraDevice = null
                    Log.e(TAG, "error = $error camera = " + camera?.id)
                }

            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun chooseOptimalSize(outputSizes: Array<Size>, width: Int, height: Int): Size {
        val preferredRatio = height / width.toDouble()
        var currentOptimalSize = outputSizes[0]
        var currentOptimalRatio = currentOptimalSize.width / currentOptimalSize.height.toDouble()
        for (currentSize in outputSizes) {
            val currentRatio = currentSize.width / currentSize.height.toDouble()
            if (Math.abs(preferredRatio - currentRatio) < Math.abs(preferredRatio - currentOptimalRatio)) {
                currentOptimalSize = currentSize
                currentOptimalRatio = currentRatio
            }
        }
        return currentOptimalSize
    }

    private fun createCameraPreview() {
        val texture = textureView!!.surfaceTexture
        texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
        val surface = Surface(texture)
        captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        cameraDevice?.createCaptureSession(Arrays.asList(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e(TAG, "onConfigureFailed")
            }

            override fun onConfigured(session: CameraCaptureSession) {
                cameraCaptureSession = session
                updatePreview()
            }

        }, null)
    }

    private fun updatePreview() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSession?.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun lock() {
        try {
            cameraCaptureSession?.capture(captureRequestBuilder.build(),
                    null, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun unlock() {
        try {
            cameraCaptureSession?.setRepeatingRequest(captureRequestBuilder.build(),
                    null, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    private fun createImageFile(galleryFolder: File): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "image_" + timeStamp + "_"
        return File.createTempFile(imageFileName, ".jpg", galleryFolder)
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera background thread")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null

        cameraDevice?.close()
        cameraDevice = null
    }
}

