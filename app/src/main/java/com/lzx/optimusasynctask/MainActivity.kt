package com.lzx.optimusasynctask

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lzx.optimustask.OptimusTask
import com.lzx.optimustask.OptimusTaskManager
import com.lzx.optimustask.TaskPriority
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {


    var taskManager1: OptimusTaskManager? = null
    var taskManager2: OptimusTaskManager? = null
    var taskManager3: OptimusTaskManager? = null
    var taskManager4: OptimusTaskManager? = null
    var taskManager5: OptimusTaskManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        OptimusTaskManager.DEBUG = true
        taskManager1 = OptimusTaskManager()
        taskManager2 = OptimusTaskManager()
        taskManager3 = OptimusTaskManager()
        taskManager4 = OptimusTaskManager()
        taskManager5 = OptimusTaskManager()

        btn1.setOnClickListener {
            taskManager1?.addTask(task = Task1(text1))
        }

        btn2.setOnClickListener {
            taskManager2?.addTask(task = Task2(text2))
        }
        btn3.setOnClickListener {
            taskManager3?.addTask(task = Task3(text3))
        }

        btn4.setOnClickListener {
            for (i in 1..10) {
                taskManager1?.addTask(task = Task1(text1))
                taskManager3?.addTask(task = Task2(text2))
                taskManager5?.addTask(task = Task2(text3))
            }
        }
    }

    private class Task1(var textView: TextView) : OptimusTask() {

        override suspend fun doTask() {
            withContext(Dispatchers.Main){
                textView.text = "执行时间不确定的任务-> " + getSequence()

                //模拟该任务耗时两秒
                textView.postDelayed({
                    doNextTask() //解除阻塞
                }, 2000)
            }

        }

        override fun finishTask() {
            Log.i("Task1", "finishTask-> " + getSequence())
        }
    }

    private class Task2(var textView: TextView) : OptimusTask() {

        override suspend fun doTask() {
            withContext(Dispatchers.Main){
                textView.text = "执行时间确定的任务-> " + getSequence()
            }

        }

        override fun finishTask() {
            Log.i("Task1", "finishTask-> " + getSequence())
        }

        override fun getDuration(): Long {
            return 2000
        }
    }


    private class Task3(var textView: TextView) : OptimusTask() {

        override suspend fun doTask() {
            withContext(Dispatchers.Main){
                textView.text = "执行时间不确定的任务(高优先级)-> " + getSequence()

                //模拟该任务耗时两秒
                textView.postDelayed({
                    doNextTask()
                }, 2000)
            }

        }

        override fun finishTask() {
            Log.i("Task1", "finishTask-> " + getSequence())
        }

        override fun getPriority(): TaskPriority {
            return TaskPriority.HIGH
        }
    }
}
