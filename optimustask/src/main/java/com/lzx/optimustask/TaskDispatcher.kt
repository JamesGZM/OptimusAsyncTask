package com.lzx.optimustask

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.annotation.NonNull

/**
 * 任务调度器，用来遍历队列
 */
class TaskDispatcher constructor(
    private val taskQueue: BlockTaskQueue,
    private val stopRunningWhenQueueEmpty: Boolean = false
) {
    var isRunning = true
    private val doTakeList: MutableList<OptimusTask> = mutableListOf()

    fun start() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            try {
                while (isRunning) {
                    //获取任务
                    val task = taskQueue.take()
                    task?.let {
                        //执行任务
                        val doEvent = TaskEvent()
                        doEvent.setTask(task)
                        doEvent.setEventType(TaskEvent.EventType.DO)
                        handler.obtainMessage(0x1000, doEvent).sendToTarget()

                        if (task.getDuration() != 0L) {
                            val finishEvent = TaskEvent()
                            finishEvent.setTask(task)
                            finishEvent.setEventType(TaskEvent.EventType.FINISH)
                            val message = Message.obtain()
                            message.what = 0x2000
                            message.obj = finishEvent
                            handler.sendMessageDelayed(message, task.getDuration())
                        }
                        task.blockTask()
                        //完成任务
                        if (task.getDuration() == 0L) {
                            val finishEvent = TaskEvent()
                            finishEvent.setTask(task)
                            finishEvent.setEventType(TaskEvent.EventType.FINISH)
                            handler.obtainMessage(0x3000, finishEvent).sendToTarget()
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(@NonNull msg: Message) {
            super.handleMessage(msg)
            val taskEvent = msg.obj as TaskEvent
            when (msg.what) {
                0x1000 -> {
                    taskEvent.getTask()!!.doTask()
                    doTakeList.add(taskEvent.getTask()!!)
                }
                0x2000 -> {
                    taskEvent.getTask()!!.unLockBlock()
                    finishTask(taskEvent)
                }
                0x3000 -> {
                    finishTask(taskEvent)
                }
                else -> {
                }
            }
        }
    }

    private fun finishTask(taskEvent: TaskEvent) {
        taskEvent.getTask()!!.finishTask()
        doTakeList.remove(taskEvent.getTask()!!)
        if (stopRunningWhenQueueEmpty && taskQueue.size() == 0) {
            isRunning = false
        }
    }

    fun clearAllTask() {
        isRunning = false
        taskQueue.clear()
        doTakeList.clear()
        handler.removeCallbacksAndMessages(null)
    }

    fun clearAndFinishAllTask() {
        isRunning = false
        taskQueue.clear()
        for (task in doTakeList) {
            task.finishTask()
        }
        doTakeList.clear()
        handler.removeCallbacksAndMessages(null)
    }
}