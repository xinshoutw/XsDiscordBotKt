package tw.xinshou.discord.plugin.giveaway.create

import tw.xinshou.discord.core.placeholder.Substitutor
import tw.xinshou.discord.core.placeholder.withUser
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.modals.Modal
import tw.xinshou.discord.plugin.giveaway.Giveaway
import tw.xinshou.discord.plugin.giveaway.Giveaway.componentId
import tw.xinshou.discord.plugin.giveaway.Giveaway.messageTemplate
import tw.xinshou.discord.plugin.giveaway.data.GiveawayPrize
import java.net.URI
import java.time.Instant

internal object StepManager {
    private data class SessionKey(val userId: Long, val guildId: Long)
    private val steps: HashMap<SessionKey, Step> = hashMapOf()
    private fun key(userId: Long, guildId: Long): SessionKey = SessionKey(userId, guildId)

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "create-giveaway") return
        val guild = event.guild ?: run {
            event.hook.editOriginal("This command can only be used in a server.").queue(); return
        }
        val step = Step(hook = event.hook, guildId = guild.idLong)
        steps[key(event.user.idLong, guild.idLong)] = step
        step.renderEmbedAction(event.userLocale).queue()
    }

    fun onButtonInteraction(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val guild = event.guild ?: run {
            event.reply(getSessionExpiredText(event.userLocale)).setEphemeral(true).queue(); return
        }
        val sessionKey = key(event.user.idLong, guild.idLong)
        val step = steps[sessionKey] ?: run {
            event.reply(getSessionExpiredText(event.userLocale)).setEphemeral(true).queue(); return
        }

        when (idMap["sub_action"]) {
            "set-title" -> showModal(event, "set-title", "Title", step.data.title)
            "set-description" -> showModal(event, "set-description", "Description", step.data.description)
            "set-end-time" -> showModal(event, "set-end-time", "End Time (yyyy-MM-dd HH:mm)",
                if (step.data.endAtEpochSecond > 0) Step.formatDateTime(step.data.endAtEpochSecond) else "")
            "add-prize" -> showPrizeModal(event, step)
            "remove-prize" -> { if (!step.removeLastPrize()) { event.reply("No prize to remove.").setEphemeral(true).queue(); return }; event.deferEdit().flatMap { step.renderEmbedAction(event.userLocale) }.queue() }
            "toggle-duplicate-policy" -> { step.toggleWinnerDuplicatePolicy(); event.deferEdit().flatMap { step.renderEmbedAction(event.userLocale) }.queue() }
            "set-sponsor" -> showModal(event, "set-sponsor", "Sponsor", step.data.sponsor)
            "set-thumbnail" -> showModal(event, "set-thumbnail", "Thumbnail URL", step.data.thumbnailUrl)
            "confirm-create" -> confirmCreate(event, step, sessionKey)
            "cancel" -> cancelCreate(event, sessionKey)
        }
    }

    fun onModalInteraction(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val guild = event.guild ?: run {
            event.reply(getSessionExpiredText(event.userLocale)).setEphemeral(true).queue(); return
        }
        val step = steps[key(event.user.idLong, guild.idLong)] ?: run {
            event.reply(getSessionExpiredText(event.userLocale)).setEphemeral(true).queue(); return
        }

        when (idMap["sub_action"]) {
            "set-title" -> { step.data.title = event.getValue("value")?.asString?.trim().orEmpty() }
            "set-description" -> { step.data.description = event.getValue("value")?.asString?.trim().orEmpty() }
            "set-end-time" -> {
                val raw = event.getValue("value")?.asString?.trim().orEmpty()
                val epochSecond = try { Step.parseDateTime(raw) } catch (_: Exception) {
                    event.reply("Invalid time format.").setEphemeral(true).queue(); return
                }
                if (epochSecond <= Instant.now().epochSecond) {
                    event.reply("End time must be in the future.").setEphemeral(true).queue(); return
                }
                step.data.endAtEpochSecond = epochSecond
            }
            "add-prize" -> {
                val prizeName = event.getValue("prize-name")?.asString?.trim().orEmpty()
                val winnerCount = event.getValue("winner-count")?.asString?.trim()?.toIntOrNull()
                if (prizeName.isBlank() || winnerCount == null || winnerCount !in 1..50) {
                    event.reply("Invalid prize data.").setEphemeral(true).queue(); return
                }
                step.data.prizes += GiveawayPrize(prizeName, winnerCount)
            }
            "set-sponsor" -> { step.data.sponsor = event.getValue("value")?.asString?.trim().orEmpty() }
            "set-thumbnail" -> {
                val thumbnail = event.getValue("value")?.asString?.trim().orEmpty()
                if (thumbnail.isNotBlank() && !isValidHttpUrl(thumbnail)) {
                    event.reply("Invalid URL.").setEphemeral(true).queue(); return
                }
                step.data.thumbnailUrl = thumbnail
            }
        }
        event.deferEdit().flatMap { step.renderEmbedAction(event.userLocale) }.queue()
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        steps.entries.removeIf { (_, step) -> step.guildId == event.guild.idLong }
    }

    private fun showModal(event: ButtonInteractionEvent, subAction: String, label: String, defaultValue: String) {
        val modalId = componentId.build("action" to "create", "sub_action" to subAction)
        val input = TextInput.create("value", TextInputStyle.SHORT)
            .setValue(defaultValue)
            .setRequired(subAction != "set-description" && subAction != "set-sponsor" && subAction != "set-thumbnail")
            .build()
        event.replyModal(Modal.create(modalId, label).addComponents(Label.of(label, input)).build()).queue()
    }

    private fun showPrizeModal(event: ButtonInteractionEvent, step: Step) {
        val modalId = componentId.build("action" to "create", "sub_action" to "add-prize")
        val nameInput = TextInput.create("prize-name", TextInputStyle.SHORT)
            .setValue("Prize ${step.data.prizes.size + 1}").setRequired(true).build()
        val countInput = TextInput.create("winner-count", TextInputStyle.SHORT)
            .setValue("1").setRequired(true).build()
        event.replyModal(Modal.create(modalId, "Add Prize")
            .addComponents(Label.of("Prize Name", nameInput))
            .addComponents(Label.of("Winner Count", countInput))
            .build()).queue()
    }

    private fun confirmCreate(event: ButtonInteractionEvent, step: Step, sessionKey: SessionKey) {
        val errorText = step.validate(event.userLocale)
        if (errorText != null) { event.reply(errorText).setEphemeral(true).queue(); return }
        val guild = event.guild ?: run { event.reply("Guild not found.").setEphemeral(true).queue(); return }
        if (event.member?.hasPermission(Permission.ADMINISTRATOR) != true) {
            event.reply("Administrator permission required.").setEphemeral(true).queue(); return
        }
        event.deferEdit().queue()
        Giveaway.createGiveaway(guild.idLong, event.channel, event.user.idLong, step.data, event.userLocale).queue({ message ->
            steps.remove(sessionKey)
            step.hook.editOriginal(
                messageTemplate.buildEdit("create-finished", event.userLocale).build()
            ).queue()
        }, { throwable ->
            step.hook.editOriginal(
                messageTemplate.buildEdit("create-failed", event.userLocale).build()
            ).queue()
        })
    }

    private fun cancelCreate(event: ButtonInteractionEvent, sessionKey: SessionKey) {
        val step = steps.remove(sessionKey) ?: run {
            event.reply(getSessionExpiredText(event.userLocale)).setEphemeral(true).queue(); return
        }
        event.deferEdit().flatMap {
            step.hook.editOriginal(messageTemplate.buildEdit("create-cancelled", event.userLocale).build())
        }.queue()
    }

    private fun getSessionExpiredText(locale: net.dv8tion.jda.api.interactions.DiscordLocale): String =
        if (locale.locale.startsWith("zh")) "建立流程已過期，請重新執行 /create-giveaway。"
        else "Your setup session expired. Please run /create-giveaway again."

    private fun isValidHttpUrl(value: String): Boolean = runCatching {
        val uri = URI(value); val scheme = uri.scheme?.lowercase()
        (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}
