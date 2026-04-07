package ntustcourse.api

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class CourseMonitorService {
    private val subscribers = ConcurrentHashMap<String, MutableList<(CourseEvent) -> Unit>>()
    private var previousState = mapOf<String, Course>()
    private var job: Job? = null

    /**
     * Register a subscription.
     * @param courseNo The course number, or "ALL" to subscribe to all courses.
     */
    fun subscribe(courseNo: String, callback: (CourseEvent) -> Unit) {
        subscribers.computeIfAbsent(courseNo) { mutableListOf() }.add(callback)
    }

    /**
     * Start monitoring.
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
                val delayTime = (1500L - elapsedTime).coerceAtLeast(100L)
                delay(delayTime)
            }
        }
    }

    /**
     * Stop monitoring.
     */
    fun stop() {
        job?.cancel()
    }

    /**
     * Comparison logic and trigger notifications.
     */
    private fun processUpdates(newList: List<Course>) {
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
     * Send notifications to corresponding subscribers.
     */
    private fun notifySubscribers(courseId: String, event: CourseEvent) {
        subscribers[courseId]?.forEach { it.invoke(event) }
        subscribers["ALL"]?.forEach { it.invoke(event) }
    }
}
