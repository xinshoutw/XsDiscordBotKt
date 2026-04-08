package tw.xinshou.discord.plugin.ntustcourse

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import tw.xinshou.discord.plugin.ntustcourse.api.CourseEvent
import tw.xinshou.discord.plugin.ntustcourse.api.CourseMonitorService
import org.koin.java.KoinJavaComponent.getKoin
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

object NtustCourse {
    // courseId[channelId[userId]]
    private val userRecordMap = ConcurrentHashMap<String, HashMap<Long, MutableList<String>>>()
    private val monitorService = CourseMonitorService()

    fun start() {
        monitorService.start()
        monitorService.subscribe("ALL") { processEvent(it) }

        // TODO: Migrate MongoDB cache access to v4 DatabaseProvider pattern
        // The old CacheDbManager/ICacheDb API no longer exists.
        // For now, userRecordMap starts empty each restart.
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild) {
            event.hook.editOriginal("This command can only be used in guilds.").queue()
            return
        }

        if (!event.channelType.isMessage) {
            event.hook.editOriginal("This command can only be used in text channels.").queue()
            return
        }

        when (event.name) {
            "course-add" -> {
                addCourse(event)
            }

            "course-remove" -> {
                removeCourse(event)
            }

            "course-list" -> {
                listCourse(event)
            }
        }
    }

    fun addCourse(event: SlashCommandInteractionEvent) {
        val courseId = event.getOption("id")!!.asString
        val userId = event.user.id

        userRecordMap.getOrPut(courseId) { hashMapOf() }
            .getOrPut(event.channelIdLong) { mutableListOf() }
            .add(userId)
        event.hook.editOriginal("You have registered this course.").queue()
    }

    fun removeCourse(event: SlashCommandInteractionEvent) {
        val courseId = event.getOption("id")!!.asString
        val userId = event.user.id

        userRecordMap[courseId]?.remove(event.channelIdLong)?.remove(userId)
        event.hook.editOriginal("You have unregistered this course.").queue()
    }

    fun listCourse(event: SlashCommandInteractionEvent) {
        val courseIds: MutableList<String> = mutableListOf()
        val userId = event.user.id
        val channelId = event.channelIdLong

        userRecordMap.forEach { (courseId, channelData) ->
            channelData[channelId]?.forEach { userIdList ->
                if (userIdList.contains(userId)) {
                    courseIds.add(courseId)
                }
            }
        }

        event.hook.editOriginal("Your registered courses: ${courseIds.joinToString(", ")}").queue()
    }

    fun processEvent(event: CourseEvent) {
        if (userRecordMap.isEmpty()) return

        // if time not after 2025/12/26 9:00, return
        val now = LocalDateTime.now()
        val targetTime = LocalDateTime.of(2025, 12, 26, 9, 0)
        if (now.isBefore(targetTime)) return

        val jda: JDA = getKoin().get()

        when (event) {
            is CourseEvent.Available -> {
                val courseId = event.course.id
                val channelMap = userRecordMap[courseId] ?: return

                for ((channelId, userIdList) in channelMap) {
                    val channel = jda.getGuildChannelById(channelId) as TextChannel? ?: continue
                    val userMention = userIdList.joinToString(" ") { "<@$it>" }
                    channel.sendMessage(
                        MessageCreateBuilder()
                            .mentionUsers(userIdList)
                            .setContent(
                                "${now.second} ||${userMention}||\n" +
                                        "[Course Available: ${event.course.name}](https://courseselection.ntust.edu.tw/First/A06/A06) \n```\n${event.course.id}\n```"
                            )
                            .build()
                    ).queue()
                }
            }

            is CourseEvent.BecameFull -> {
                val courseId = event.course.id
                val channelMap = userRecordMap[courseId] ?: return

                for ((channelId, userIdList) in channelMap) {
                    val channel = jda.getGuildChannelById(channelId) as TextChannel? ?: continue
                    channel.sendMessage(
                        MessageCreateBuilder()
                            .mentionUsers(userIdList)
                            .setContent("Course Became Full: ${event.course.name} (${event.course.id})")
                            .build()
                    ).queue()
                }
            }
        }

    }

    fun stop() {
        monitorService.stop()
    }
}
