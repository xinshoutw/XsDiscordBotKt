package tw.xinshou.discord.plugin.ntustcourse.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Serializable
data class CourseQueryPayload(
    @SerialName("Semester") val semester: String,
    @SerialName("CourseNo") val courseNo: String,
    @SerialName("CourseName") val courseName: String,
    @SerialName("CourseTeacher") val courseTeacher: String,
    @SerialName("Dimension") val dimension: String,
    @SerialName("CourseNotes") val courseNotes: String,
    @SerialName("CampusNotes") val campusNotes: String,
    @SerialName("ForeignLanguage") val foreignLanguage: Int,
    @SerialName("OnlyGeneral") val onlyGeneral: Int,
    @SerialName("OnleyNTUST") val onlyNtust: Int,
    @SerialName("OnlyMaster") val onlyMaster: Int,
    @SerialName("OnlyUnderGraduate") val onlyUndergraduate: Int,
    @SerialName("OnlyNode") val onlyNode: Int,
    @SerialName("Language") val language: String
)

@Serializable
data class Course(
    @SerialName("CourseNo") val id: String,
    @SerialName("CourseName") val name: String,
    @SerialName("ChooseStudent") val currentCount: Int,
    @SerialName("Restrict2") val limitStr: String,
    @SerialName("CourseTeacher") val teacher: String? = null
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

    private val client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun searchAllCourses(semester: String): List<Course> = withContext(Dispatchers.IO) {
        val payload = CourseQueryPayload(
            semester = semester, courseNo = "", courseName = "", courseTeacher = " ",
            dimension = "", courseNotes = "", campusNotes = "", foreignLanguage = 0,
            onlyGeneral = 0, onlyNtust = 0, onlyMaster = 0, onlyUndergraduate = 0,
            onlyNode = 0, language = "zh"
        )

        val jsonBody = json.encodeToString(payload)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("content-type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                return@withContext json.decodeFromString<List<Course>>(response.body())
            }
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
        }
        return@withContext emptyList()
    }
}
