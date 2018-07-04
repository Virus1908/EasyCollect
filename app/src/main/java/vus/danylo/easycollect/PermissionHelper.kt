package vus.danylo.easycollect

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog

private const val PERMISSIONS_REQUEST_CODE: Int = 69

class PermissionHelper {
    interface PermissionAllowedListener {
        fun onPermissionAllowed()
        fun onPermissionDisallowed()
    }

    private var listener: PermissionAllowedListener? = null

    fun checkPermission(activity: Activity, fragment: Fragment) {
        val cameraPermission = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.CAMERA)
        val writePermission = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (cameraPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.CAMERA)) {
                showConfirmPermissionDialog(fragment)
            } else {
                makeRequest(fragment)
            }
        } else {
            listener?.onPermissionAllowed()
        }
    }

    private fun makeRequest(fragment: Fragment) {
        fragment.requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_CODE)
    }

    fun onRequestPermissionsResult(fragment: Fragment, requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size != 2 || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                showNoPermissionDialog(fragment)
            } else {
                listener?.onPermissionAllowed()
            }
            return true
        }
        return false
    }

    private fun showConfirmPermissionDialog(fragment: Fragment) {
        fragment.context?.let {
            AlertDialog.Builder(it)
                    .setMessage(R.string.confirm_permissions_text)
                    .setPositiveButton(R.string.ok, (DialogInterface.OnClickListener { _, _ -> makeRequest(fragment) }))
                    .create().show()
        }

    }

    private fun showNoPermissionDialog(fragment: Fragment) {
        fragment.context?.let {
            AlertDialog.Builder(it)
                    .setMessage(R.string.no_permissions_text)
                    .setTitle(R.string.no_permissions_title)
                    .setPositiveButton(R.string.ok, (DialogInterface.OnClickListener { _, _ -> listener?.onPermissionDisallowed() }))
                    .create().show()
        }

    }

    fun setPermissionAllowedListener(listener: PermissionAllowedListener) {
        this.listener = listener
    }
}