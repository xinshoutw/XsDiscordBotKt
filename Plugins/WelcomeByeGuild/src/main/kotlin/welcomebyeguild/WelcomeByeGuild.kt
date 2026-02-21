package tw.xinshou.discord.plugin.welcomebyeguild

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.core.builtin.messagecreator.modal.ModalCreator
import tw.xinshou.discord.core.builtin.messagecreator.v2.MessageCreator
import tw.xinshou.discord.core.json.JsonFileManager
import tw.xinshou.discord.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.discord.core.json.JsonGuildFileManager
import tw.xinshou.discord.core.util.ComponentIdManager
import tw.xinshou.discord.core.util.FieldType
import tw.xinshou.discord.plugin.welcomebyeguild.Event.componentPrefix
import tw.xinshou.discord.plugin.welcomebyeguild.Event.pluginDirectory
import java.io.File

internal object WelcomeByeGuild {
    internal object Actions {
        const val SELECT_CHANNEL = "select-channel"
        const val MODAL_WELCOME_TEXT = "modal-welcome-text"
        const val MODAL_BYE_TEXT = "modal-bye-text"
        const val MODAL_IMAGES = "modal-images"
        const val MODAL_COLORS = "modal-colors"
        const val PREVIEW_JOIN = "preview-join"
        const val PREVIEW_LEAVE = "preview-leave"
        const val CONFIRM_CREATE = "confirm-create"
    }

    internal object Inputs {
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val THUMBNAIL = "thumbnail"
        const val IMAGE = "image"
        const val WELCOME_COLOR = "welcome-color"
        const val BYE_COLOR = "bye-color"
    }

    internal object MessageKeys {
        const val GUILD_ONLY = "guild-only"
        const val INVALID_COLOR = "invalid-color"
        const val CHANNEL_NOT_SET = "channel-not-set"

        const val SETUP_PANEL = "setup-panel"
        const val SETUP_SAVED = "setup-saved"

        const val DEFAULT_JOIN = "default-join"
        const val DEFAULT_LEAVE = "default-leave"
    }

    internal object ModalKeys {
        const val WELCOME_TEXT = "welcome-text"
        const val LEAVE_TEXT = "leave-text"
        const val IMAGES = "images"
        const val COLORS = "colors"
    }

    internal object Models {
        const val PREVIEW_EMBED = "wbg@preview-embed"
    }

    internal val colorRegex = Regex("^#?[0-9a-fA-F]{6}$")
    internal val defaultLocale: DiscordLocale = DiscordLocale.CHINESE_TAIWAN

    internal val jsonAdapter: JsonAdapter<GuildSetting> = JsonFileManager.moshi.adapterReified<GuildSetting>()
    internal val jsonGuildManager = JsonGuildFileManager(
        dataDirectory = File(pluginDirectory, "data"),
        adapter = jsonAdapter,
        defaultInstance = GuildSetting()
    )

    internal val componentIdManager = ComponentIdManager(
        prefix = componentPrefix,
        idKeys = mapOf(
            "action" to FieldType.STRING,
        )
    )

    internal lateinit var messageCreator: MessageCreator
    internal lateinit var modalCreator: ModalCreator
    internal val steps: MutableMap<Long, CreateStep> = hashMapOf()

    internal fun load() {
        messageCreator = MessageCreator(
            pluginDirFile = pluginDirectory,
            defaultLocale = defaultLocale,
            componentIdManager = componentIdManager,
        )

        modalCreator = ModalCreator(
            langDirFile = File(pluginDirectory, "lang"),
            componentIdManager = componentIdManager,
            defaultLocale = defaultLocale
        )
    }

    internal fun reload() {
        steps.clear()
        load()
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        jsonGuildManager.removeAndSave(event.guild.idLong)
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        handleSlashCommandInteraction(event)
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        handleButtonInteraction(event)
    }

    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        handleEntitySelectInteraction(event)
    }

    fun onModalInteraction(event: ModalInteractionEvent) {
        handleModalInteraction(event)
    }

    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        handleGuildMemberJoin(event)
    }

    fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        handleGuildMemberRemove(event)
    }
}
