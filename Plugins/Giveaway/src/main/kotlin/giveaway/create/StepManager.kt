package tw.xinshou.discord.plugin.giveaway.create

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.plugin.giveaway.Giveaway
import tw.xinshou.discord.plugin.giveaway.Giveaway.messageCreator
import tw.xinshou.discord.plugin.giveaway.Giveaway.modalCreator
import tw.xinshou.discord.plugin.giveaway.data.GiveawayPrize
import java.net.URI
import java.time.Instant

internal object StepManager {
    private data class SessionKey(
        val userId: Long,
        val guildId: Long,
    )

    private val steps: HashMap<SessionKey, Step> = hashMapOf()

    private fun key(userId: Long, guildId: Long): SessionKey = SessionKey(userId, guildId)

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "create-giveaway") return

        val guild = event.guild
        if (guild == null) {
            event.hook.editOriginal("This command can only be used in a server.").queue()
            return
        }

        val step = Step(
            hook = event.hook,
            guildId = guild.idLong,
        )

        steps[key(event.user.idLong, guild.idLong)] = step
        step.renderEmbedAction(event.userLocale).queue()
    }

    fun onButtonInteraction(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val guild = event.guild ?: run {
            event.reply(getSessionExpiredText(event.userLocale)).setEphemeral(true).queue()
            return
        }

        val sessionKey = key(event.user.idLong, guild.idLong)
        val step = steps[sessionKey] ?: run {
            event.reply(getSessionExpiredText(event.userLocale)).setEphemeral(true).queue()
            return
        }

        when (idMap["sub_action"]) {
            "set-title" -> setTitleForm(event, step)
            "set-description" -> setDescriptionForm(event, step)
            "set-end-time" -> setEndTimeForm(event, step)
            "add-prize" -> addPrizeForm(event, step)
            "remove-prize" -> removePrize(event, step)
            "toggle-duplicate-policy" -> toggleDuplicatePolicy(event, step)
            "set-sponsor" -> setSponsorForm(event, step)
            "set-thumbnail" -> setThumbnailForm(event, step)
            "confirm-create" -> confirmCreate(event, step, sessionKey)
            "cancel" -> cancelCreate(event, sessionKey)
        }
    }

    fun onModalInteraction(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val guild = event.guild ?: run {
            event.reply(getSessionExpiredText(event.userLocale)).setEphemeral(true).queue()
            return
        }

        val step = steps[key(event.user.idLong, guild.idLong)] ?: run {
            event.reply(getSessionExpiredText(event.userLocale)).setEphemeral(true).queue()
            return
        }

        when (idMap["sub_action"]) {
            "set-title" -> {
                val value = event.getValue("title")?.asString?.trim().orEmpty()
                if (value.isBlank()) {
                    event.reply(getTitleRequiredText(event.userLocale)).setEphemeral(true).queue()
                    return
                }
                step.data.title = value
            }

            "set-description" -> {
                step.data.description = event.getValue("description")?.asString?.trim().orEmpty()
            }

            "set-end-time" -> {
                val raw = event.getValue("end-time")?.asString?.trim().orEmpty()
                if (raw.isBlank()) {
                    event.reply(getEndTimeRequiredText(event.userLocale)).setEphemeral(true).queue()
                    return
                }

                val epochSecond = try {
                    Step.parseDateTime(raw)
                } catch (_: Exception) {
                    event.reply(getEndTimeFormatText(event.userLocale)).setEphemeral(true).queue()
                    return
                }

                if (epochSecond <= Instant.now().epochSecond) {
                    event.reply(getEndTimeFutureText(event.userLocale)).setEphemeral(true).queue()
                    return
                }

                step.data.endAtEpochSecond = epochSecond
            }

            "add-prize" -> {
                val prizeName = event.getValue("prize-name")?.asString?.trim().orEmpty()
                val winnerCount = event.getValue("winner-count")?.asString?.trim()?.toIntOrNull()

                if (prizeName.isBlank()) {
                    event.reply(getPrizeNameRequiredText(event.userLocale)).setEphemeral(true).queue()
                    return
                }

                if (winnerCount == null || winnerCount !in 1..50) {
                    event.reply(getWinnerCountRangeText(event.userLocale)).setEphemeral(true).queue()
                    return
                }

                step.data.prizes += GiveawayPrize(prizeName, winnerCount)
            }

            "set-sponsor" -> {
                step.data.sponsor = event.getValue("sponsor")?.asString?.trim().orEmpty()
            }

            "set-thumbnail" -> {
                val thumbnail = event.getValue("thumbnail")?.asString?.trim().orEmpty()
                if (thumbnail.isNotBlank() && !isValidHttpUrl(thumbnail)) {
                    event.reply(getThumbnailFormatText(event.userLocale)).setEphemeral(true).queue()
                    return
                }
                step.data.thumbnailUrl = thumbnail
            }
        }

        event.deferEdit().flatMap {
            step.renderEmbedAction(event.userLocale)
        }.queue()
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        steps.entries.removeIf { (_, step) -> step.guildId == event.guild.idLong }
    }

    private fun setTitleForm(event: ButtonInteractionEvent, step: Step) {
        event.replyModal(
            modalCreator.getModalBuilder(
                key = "set-title",
                locale = event.userLocale,
                substitutor = Placeholder.get(event).put("ga@title", step.data.title),
            ).build()
        ).queue()
    }

    private fun setDescriptionForm(event: ButtonInteractionEvent, step: Step) {
        event.replyModal(
            modalCreator.getModalBuilder(
                key = "set-description",
                locale = event.userLocale,
                substitutor = Placeholder.get(event).put("ga@description", step.data.description),
            ).build()
        ).queue()
    }

    private fun setEndTimeForm(event: ButtonInteractionEvent, step: Step) {
        val currentEndTime = if (step.data.endAtEpochSecond > 0) {
            Step.formatDateTime(step.data.endAtEpochSecond)
        } else {
            ""
        }

        event.replyModal(
            modalCreator.getModalBuilder(
                key = "set-end-time",
                locale = event.userLocale,
                substitutor = Placeholder.get(event).put("ga@end-time", currentEndTime),
            ).build()
        ).queue()
    }

    private fun addPrizeForm(event: ButtonInteractionEvent, step: Step) {
        val defaultName = if (event.userLocale.locale.startsWith("zh")) {
            "獎品 ${step.data.prizes.size + 1}"
        } else {
            "Prize ${step.data.prizes.size + 1}"
        }

        event.replyModal(
            modalCreator.getModalBuilder(
                key = "add-prize",
                locale = event.userLocale,
                substitutor = Placeholder.get(event).put("ga@prize-name", defaultName),
            ).build()
        ).queue()
    }

    private fun removePrize(event: ButtonInteractionEvent, step: Step) {
        if (!step.removeLastPrize()) {
            event.reply(getNoPrizeText(event.userLocale)).setEphemeral(true).queue()
            return
        }

        event.deferEdit().flatMap {
            step.renderEmbedAction(event.userLocale)
        }.queue()
    }

    private fun toggleDuplicatePolicy(event: ButtonInteractionEvent, step: Step) {
        step.toggleWinnerDuplicatePolicy()
        event.deferEdit().flatMap {
            step.renderEmbedAction(event.userLocale)
        }.queue()
    }

    private fun setSponsorForm(event: ButtonInteractionEvent, step: Step) {
        event.replyModal(
            modalCreator.getModalBuilder(
                key = "set-sponsor",
                locale = event.userLocale,
                substitutor = Placeholder.get(event).put("ga@sponsor", step.data.sponsor),
            ).build()
        ).queue()
    }

    private fun setThumbnailForm(event: ButtonInteractionEvent, step: Step) {
        event.replyModal(
            modalCreator.getModalBuilder(
                key = "set-thumbnail",
                locale = event.userLocale,
                substitutor = Placeholder.get(event).put("ga@thumbnail", step.data.thumbnailUrl),
            ).build()
        ).queue()
    }

    private fun confirmCreate(event: ButtonInteractionEvent, step: Step, sessionKey: SessionKey) {
        val errorText = step.validate(event.userLocale)
        if (errorText != null) {
            event.reply(errorText).setEphemeral(true).queue()
            return
        }

        val guild = event.guild ?: run {
            event.reply("Guild not found.").setEphemeral(true).queue()
            return
        }

        if (event.member?.hasPermission(Permission.ADMINISTRATOR) != true) {
            event.reply(getNoPermissionText(event.userLocale)).setEphemeral(true).queue()
            return
        }

        event.deferEdit().queue()

        Giveaway.createGiveaway(
            guildId = guild.idLong,
            channel = event.channel,
            creatorId = event.user.idLong,
            config = step.data,
            locale = event.userLocale,
        ).queue({ message ->
            steps.remove(sessionKey)

            step.hook.editOriginal(
                messageCreator.getEditBuilder(
                    key = "create-finished",
                    locale = event.userLocale,
                    replaceMap = mapOf(
                        "ga@jump-url" to message.jumpUrl,
                    )
                ).build()
            ).queue()
        }, { throwable ->
            step.hook.editOriginal(
                messageCreator.getEditBuilder(
                    key = "create-failed",
                    locale = event.userLocale,
                    replaceMap = mapOf(
                        "ga@error" to (throwable.message ?: "unknown"),
                    )
                ).build()
            ).queue()
        })
    }

    private fun cancelCreate(event: ButtonInteractionEvent, sessionKey: SessionKey) {
        val step = steps.remove(sessionKey) ?: run {
            event.reply(getSessionExpiredText(event.userLocale)).setEphemeral(true).queue()
            return
        }

        event.deferEdit().flatMap {
            step.hook.editOriginal(
                messageCreator.getEditBuilder(
                    key = "create-cancelled",
                    locale = event.userLocale,
                ).build()
            )
        }.queue()
    }

    private fun getSessionExpiredText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String {
        return if (locale.locale.startsWith("zh")) {
            "建立流程已過期，請重新執行 /create-giveaway。"
        } else {
            "Your setup session expired. Please run /create-giveaway again."
        }
    }

    private fun getTitleRequiredText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String {
        return if (locale.locale.startsWith("zh")) "抽獎名稱不能為空。" else "Giveaway title cannot be empty."
    }

    private fun getEndTimeRequiredText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String {
        return if (locale.locale.startsWith("zh")) "請輸入結束時間。" else "Please provide an end time."
    }

    private fun getEndTimeFormatText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String {
        return if (locale.locale.startsWith("zh")) {
            "時間格式錯誤，請使用 yyyy-MM-dd HH:mm。"
        } else {
            "Invalid time format. Use yyyy-MM-dd HH:mm."
        }
    }

    private fun getEndTimeFutureText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String {
        return if (locale.locale.startsWith("zh")) "結束時間必須是未來時間。" else "End time must be in the future."
    }

    private fun getPrizeNameRequiredText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String {
        return if (locale.locale.startsWith("zh")) "獎品名稱不能為空。" else "Prize name cannot be empty."
    }

    private fun getWinnerCountRangeText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String {
        return if (locale.locale.startsWith("zh")) {
            "中獎人數需為 1 到 50 的整數。"
        } else {
            "Winner count must be an integer between 1 and 50."
        }
    }

    private fun getThumbnailFormatText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String {
        return if (locale.locale.startsWith("zh")) {
            "縮圖網址格式錯誤，請輸入有效的 http(s) URL。"
        } else {
            "Invalid thumbnail URL. Please provide a valid http(s) URL."
        }
    }

    private fun getNoPrizeText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String {
        return if (locale.locale.startsWith("zh")) "目前沒有可刪除的獎品。" else "No prize to remove."
    }

    private fun getNoPermissionText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String {
        return if (locale.locale.startsWith("zh")) {
            "你已失去管理員權限，無法完成建立。"
        } else {
            "Administrator permission is required to finish creation."
        }
    }

    private fun isValidHttpUrl(value: String): Boolean {
        return runCatching {
            val uri = URI(value)
            val scheme = uri.scheme?.lowercase()
            (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }
}
