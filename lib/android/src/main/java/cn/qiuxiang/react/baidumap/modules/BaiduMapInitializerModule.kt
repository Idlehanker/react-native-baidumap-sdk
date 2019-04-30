package cn.qiuxiang.react.baidumap.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.baidu.mapapi.SDKInitializer.*
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter

@Suppress("unused")
class BaiduMapInitializerModule(private val context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {

    private val mCurrentReceiver: SDKReceiver by lazy { SDKReceiver() }
    private var mInitState = 0 //是否初始化
    private var mReceiveState = 0 //是否收到通知
    private var mInitResult = false //是否初始化成功

    private val isMInit  //sdk 是否初始化
        get() = mInitState == 1

    private val isMReceive  //是否获取 key 结果
        get() = mReceiveState == 1

    private var mErrorMessage: String? = null //error message

    inner class SDKReceiver : BroadcastReceiver() {

        private val mPromiseList by lazy { arrayListOf<Promise>() }

        override fun onReceive(context: Context, intent: Intent) {
            mReceiveState = 1
            try {
                if (intent.action == SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK) {
                    mInitResult = true
                    mPromiseList.forEach { it.resolve(null) }
                } else {
                    val code = intent.getIntExtra(SDK_BROADTCAST_INTENT_EXTRA_INFO_KEY_ERROR_CODE, 0)
                    val message = code.toString()
                    mErrorMessage = message
                    mPromiseList.forEach { it.reject(message, intent.action) }
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

        context.currentActivity?.apply {

            if (!isMInit) {
                mInitState = 1
                val intentFilter = IntentFilter()
                intentFilter.addAction(SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK)
                intentFilter.addAction(SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)
                try {
                    registerReceiver(mCurrentReceiver, intentFilter)
                } catch (e: Exception) {
                    Log.e("BaiduMapModule", "register failed", e)
                }
//                initialize(context.applicationContext)
                initialize(application)
                mCurrentReceiver += promise
                return
            }

            if (!isMReceive) {
                mCurrentReceiver += promise
                return
            }

            if (mInitResult) {
                promise.resolve("success")
            } else {
                promise.reject(mErrorMessage)
            }

        }
    }

}
