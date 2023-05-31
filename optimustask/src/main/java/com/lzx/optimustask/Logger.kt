package com.lzx.optimustask

import android.util.Log

interface Logger {
    val TAG: String
    fun i(msg: String)
    fun d(msg: String)
    fun e(msg: String)
}

object DefaultLogger : Logger {

    override val TAG: String
        get() = "task"

    override fun i(msg: String) {
        if (OptimusTaskManager.DEBUG) {
            Log.i(TAG, msg)
        }
    }

    override fun d(msg: String) {
        if (OptimusTaskManager.DEBUG) {
            Log.d(TAG, msg)
        }
    }

    override fun e(msg: String) {
        if (OptimusTaskManager.DEBUG) {
            Log.e(TAG, msg)
        }
    }

}