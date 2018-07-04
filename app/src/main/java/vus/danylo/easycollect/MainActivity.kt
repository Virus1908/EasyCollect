package vus.danylo.easycollect

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedInstanceState ?: supportFragmentManager.beginTransaction()
                .replace(R.id.container, CameraFragment.newInstance())
                .commit()
    }
}