package tw.xinshou.discord.plugin.ntustcourse.api

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class CourseMonitorService {
    private val subscribers = ConcurrentHashMap<String, MutableList<(CourseEvent) -> Unit>>()
    private var previousState = mapOf<String, Course>()
    private var job: Job? = null

    /**
     * 註冊訂閱
     * @param courseNo 指定課程代碼，若為 "ALL" 則訂閱所有課程
     */
    fun subscribe(courseNo: String, callback: (CourseEvent) -> Unit) {
        subscribers.computeIfAbsent(courseNo) { mutableListOf() }.add(callback)
    }

    /**
     * 開始監控
     */
    fun start(semester: String = "1142") {
        if (job != null && job!!.isActive) return

        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val startTime = System.currentTimeMillis()
                val currentList = CourseApi.searchAllCourses(semester)

                if (currentList.isNotEmpty()) {
                    processUpdates(currentList)
                }

                val elapsedTime = System.currentTimeMillis() - startTime
                val delayTime = (1500L - elapsedTime).coerceAtLeast(100L) // 至少休眠 100ms
                delay(delayTime)
            }
        }
    }

    /**
     * 停止監控
     */
    fun stop() {
        job?.cancel()
    }

    /**
     * 比對邏輯與觸發通知
     */
    private fun processUpdates(newList: List<Course>) {
        // 將 List 轉為 Map 以便快速查找 (Key: CourseNo)
        val currentMap = newList.associateBy { it.id }

        currentMap.forEach { (id, newCourse) ->
            val oldCourse = previousState[id]

            if (!newCourse.isFull) {
                notifySubscribers(id, CourseEvent.Available(newCourse))
            }

            if (oldCourse != null && !oldCourse.isFull && newCourse.isFull) {
                notifySubscribers(id, CourseEvent.BecameFull(newCourse, oldCourse.currentCount))
            }
        }

        previousState = currentMap
    }

    /**
     * 發送通知給對應的訂閱者
     */
    private fun notifySubscribers(courseId: String, event: CourseEvent) {
        subscribers[courseId]?.forEach { it.invoke(event) }
        subscribers["ALL"]?.forEach { it.invoke(event) }
    }
}