package cn.qiuxiang.react.baidumap.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.SDKInitializer.*
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter

@Suppress("unused")
class BaiduMapInitializerModule(private val context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {

    private val mCurrentReceiver: SDKReceiver by lazy { SDKReceiver() }

    init {
        Handler(Looper.getMainLooper()).post {
            SDKInitializer.initialize(context.applicationContext)
        }
    }

    class SDKReceiver : BroadcastReceiver() {

        private val mPromiseList by lazy { arrayListOf<Promise>() }

        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (intent.action == SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK) {
                    mPromiseList.forEach { it.resolve(null) }
                } else {
                    val code = intent.getIntExtra(SDK_BROADTCAST_INTENT_EXTRA_INFO_KEY_ERROR_CODE, 0)
                    mPromiseList.forEach { it.reject(code.toString(), intent.action) }
                }
            } catch (e: Exception) {
                Log.e("BaiduMapModule", "handle baidu message error", e)
            } finally {
                mPromiseList.clear()
            }

        }

        operator fun plusAssign(promise: Promise) {
            mPromiseList.add(promise)
        }
    }

    private val emitter by lazy { context.getJSModule(RCTDeviceEventEmitter::class.java) }

    override fun getName(): String {
        return "BaiduMapInitializer"
    }

    override fun canOverrideExistingModule(): Boolean {
        return true
    }

    @ReactMethod
    fun init(promise: Promise) {
        //

        context.currentActivity?.apply {
            val intentFilter = IntentFilter()
            intentFilter.addAction(SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK)
            intentFilter.addAction(SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)
            try {
                unregisterReceiver(mCurrentReceiver)
            } catch (e: Exception) {
                Log.e("BaiduMapModule", "unregister failed", e)
            }
            registerReceiver(mCurrentReceiver, intentFilter)
            mCurrentReceiver += promise
        }

    }


}
