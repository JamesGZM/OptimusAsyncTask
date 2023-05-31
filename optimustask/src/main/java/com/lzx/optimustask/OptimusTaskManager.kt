package com.lzx.optimustask

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import java.util.*
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class OptimusTaskManager {

    companion object {
        internal val cacheTaskNameList = mutableListOf<String>()

        var DEBUG = false
        var logger: Logger = DefaultLogger
        var currRunningTask: IOptimusTask? = null
    }

    private var channel = Channel<IOptimusTask>(Channel.UNLIMITED)

    //使用SupervisorJob 的 coroutineScope, 异常不会取消父协程
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val atomicInteger = AtomicInteger()
    private val deferred = PriorityBlockingQueue<Int>()

    private val handler = CoroutineExceptionHandler { _, exception ->
        logger.i("handlerCatch -> $exception")
    }

    init {
        loop()
    }

    fun getRunningTask() = currRunningTask

    fun addTask(task: IOptimusTask) {
        scope.launch(handler) { addTaskSuspend(task) }
    }

    private suspend fun addTaskSuspend(task: IOptimusTask) {
        task.setDeferred(deferred)
        logger.i("addTask name = " + task.getTaskName())
        val result = if (checkChannelActive()) {
            sendTask(task)
        } else {
            reset()
            reSendTaskIfNeed() //重新发送缓存任务
            sendTask(task)     //最后发送当前任务
        }
        logger.i("sendTask result = $result")
    }

    private suspend fun sendTask(task: IOptimusTask): Boolean {
        return runCatching {
            if (checkChannelActive()) {
                task.setSequence(atomicInteger.incrementAndGet())
                TaskQueueManager.addTask(task)
                channel.send(task)

                logger.i("send task -> ${task.getTaskName()}")
                true
            } else {
                logger.i("Channel is not Active，removeTask -> ${task.getTaskName()}")
                TaskQueueManager.removeTask(task)
                cacheTaskNameList.remove(task.getTaskName())
                return false
            }
        }.onFailure {
            logger.i("addTaskCatch -> $it")
            TaskQueueManager.removeTask(task)
            cacheTaskNameList.remove(task.getTaskName())
        }.getOrElse { false }
    }

    private fun loop() {
        scope.launch(handler) {
            channel.consumeEach {
                tryToHandlerTask(it)
            }
        }
    }

    /**
     * 重置数据
     */
    private fun reset() {
        channel = Channel(Channel.BUFFERED)
        loop()
    }

    private suspend fun reSendTaskIfNeed() {
        runCatching {
            if (TaskQueueManager.getTaskQueueSize() > 0) {
                val list = Collections.synchronizedList(mutableListOf<IOptimusTask>())
                TaskQueueManager.getTaskQueue().forEach { list.add(it) }
                TaskQueueManager.clearTaskQueue()
                list.forEach { sendTask(it) }
            }
        }
    }

    private suspend fun tryToHandlerTask(it: IOptimusTask) {
        try {
            logger.i("tryToHandlerTask -> ${it.getTaskName()}")
            currRunningTask = it
            it.doTask()
            if (it.getDuration() != 0L) {
                delay(it.getDuration())
                withContext(Dispatchers.Main) { it.finishTask() }
                currRunningTask = null

                logger.i("tryToHandlerTask finish，removeTask -> ${it.getTaskName()}")
                TaskQueueManager.removeTask(it)
                cacheTaskNameList.remove(it.getTaskName())
            } else {
                deferred.take()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            logger.i("handlerTaskCatch -> $ex")
        }
    }

    /**
     * 检查管道是否激活
     */
    private fun checkChannelActive(): Boolean {
        return !channel.isClosedForReceive && !channel.isClosedForSend
    }


    fun clear() {
        runCatching {
            deferred.add(1)
            channel.close()
            currRunningTask?.finishTask()
            cacheTaskNameList.clear()
            TaskQueueManager.clearTaskQueue()
            currRunningTask = null
        }
    }
}
