package vus.danylo.easycollect

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.camera_fragment.*
import java.io.File

class CameraFragment : Fragment(), PermissionHelper.PermissionAllowedListener {

    private val permissionHelper = PermissionHelper()
    private val cameraHelper = CameraHelper()

    private lateinit var galleryFolder: File

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.camera_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        permissionHelper.setPermissionAllowedListener(this)
        activity?.let { permissionHelper.checkPermission(it, this) }
        take_photo.setOnClickListener { button: View -> cameraHelper.takePicture(button.context) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!permissionHelper.onRequestPermissionsResult(this, requestCode, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onResume() {
        super.onResume()
        cameraHelper.resume()
    }

    override fun onPause() {
        cameraHelper.pause()
        super.onPause()
    }

    override fun onPermissionDisallowed() {
        activity?.finish()
    }

    override fun onPermissionAllowed() {
        createImageGallery()
        cameraHelper.setCameraManager(activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager)
        cameraHelper.setTextureView(texture)
    }

    private fun createImageGallery() {
        val storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        galleryFolder = File(storageDirectory, resources.getString(R.string.app_name))
        if (!galleryFolder.exists()) {
            val wasCreated = galleryFolder.mkdirs()
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory")
            }
        }
        cameraHelper.setGalleryFolder(galleryFolder)
    }

    companion object {
        @JvmStatic
        fun newInstance(): CameraFragment {
            return CameraFragment()
        }
    }
}
