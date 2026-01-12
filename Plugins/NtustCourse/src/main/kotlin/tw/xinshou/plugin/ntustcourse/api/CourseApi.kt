package tw.xinshou.plugin.ntustcourse.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@JsonClass(generateAdapter = true)
data class CourseQueryPayload(
    @param:Json(name = "Semester") val semester: String,
    @param:Json(name = "CourseNo") val courseNo: String,
    @param:Json(name = "CourseName") val courseName: String,
    @param:Json(name = "CourseTeacher") val courseTeacher: String,
    @param:Json(name = "Dimension") val dimension: String,
    @param:Json(name = "CourseNotes") val courseNotes: String,
    @param:Json(name = "CampusNotes") val campusNotes: String,
    @param:Json(name = "ForeignLanguage") val foreignLanguage: Int,
    @param:Json(name = "OnlyGeneral") val onlyGeneral: Int,
    @param:Json(name = "OnleyNTUST") val onlyNtust: Int, // 注意：保留 API 的拼寫錯誤 "Onley"
    @param:Json(name = "OnlyMaster") val onlyMaster: Int,
    @param:Json(name = "OnlyUnderGraduate") val onlyUndergraduate: Int,
    @param:Json(name = "OnlyNode") val onlyNode: Int,
    @param:Json(name = "Language") val language: String
)

@JsonClass(generateAdapter = true)
data class Course(
    @param:Json(name = "CourseNo") val id: String,
    @param:Json(name = "CourseName") val name: String,
    @param:Json(name = "ChooseStudent") val currentCount: Int, // 已選人數
    @param:Json(name = "Restrict2") val limitStr: String,      // 限選人數 (API 回傳通常是字串)
    @param:Json(name = "CourseTeacher") val teacher: String?
) {
    val limit: Int get() = limitStr.toIntOrNull() ?: 0
    val isFull: Boolean get() = currentCount >= limit
}

sealed class CourseEvent {
    data class Available(val course: Course) : CourseEvent()
    data class BecameFull(val course: Course, val previousCount: Int) : CourseEvent()
}

object CourseApi {
    private const val API_URL = "https://querycourse.ntust.edu.tw/QueryCourse/api//courses"
//    private const val API_URL = "https://querycourse.xinshou.tw/querycourse/api/courses" // test server

    private val client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(5)) // 超時縮短，因為要頻繁請求
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val responseType = Types.newParameterizedType(List::class.java, Course::class.java)
    private val responseAdapter = moshi.adapter<List<Course>>(responseType)
    private val requestAdapter = moshi.adapter(CourseQueryPayload::class.java)

    suspend fun searchAllCourses(semester: String): List<Course> = withContext(Dispatchers.IO) {
        val payload = CourseQueryPayload(
            semester = semester, courseNo = "", courseName = "", courseTeacher = " ", // 關鍵：搜尋全部
            dimension = "", courseNotes = "", campusNotes = "", foreignLanguage = 0,
            onlyGeneral = 0, onlyNtust = 0, onlyMaster = 0, onlyUndergraduate = 0,
            onlyNode = 0, language = "zh"
        )

        val jsonBody = requestAdapter.toJson(payload)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("content-type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                return@withContext responseAdapter.fromJson(response.body()) ?: emptyList()
            }
        } catch (e: Exception) {
            println("請求失敗: ${e.message}")
        }
        return@withContext emptyList()
    }
}