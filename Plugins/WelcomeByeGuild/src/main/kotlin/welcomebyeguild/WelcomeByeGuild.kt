package welcomebyeguild

import core.i18n.MessageTemplate
import core.placeholder.Substitutor
import core.util.GuildJsonFile
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageEditData
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@Serializable
internal data class GuildSetting(
    var welcomeChannelId: Long = 0L,
    var byeChannelId: Long = 0L,
)

internal object WelcomeByeGuild {
    private val defaultLocale: DiscordLocale = DiscordLocale.CHINESE_TAIWAN

    private fun createMessageTemplate(): MessageTemplate = MessageTemplate(
        langDir = File(Event.pluginDirectory, "lang"),
        defaultLocale = defaultLocale,
    )

    private val jsonGuildManager = GuildJsonFile(
        directory = File(Event.pluginDirectory, "data"),
        serializer = GuildSetting.serializer(),
        defaultInstance = { GuildSetting() },
    )

    private val messageTemplateRef = AtomicReference(createMessageTemplate())

    internal fun load() {
        messageTemplateRef.set(createMessageTemplate())
    }

    internal fun reload() {
        messageTemplateRef.set(createMessageTemplate())
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        // GuildJsonFile doesn't have removeAndSave, but we can delete the file
        val file = File(File(Event.pluginDirectory, "data"), "${event.guild.idLong}.json")
        if (file.exists()) file.delete()
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            reply(event, WelcomeByeGuildMessageKeys.GUILD_ONLY)
            return
        }

        if (!hasAdminPermission(event)) {
            reply(
                event,
                WelcomeByeGuildMessageKeys.NO_PERMISSION,
                WelcomeByeGuildSubstitutorFactory.forCommand(event, guild, jsonGuildManager[guild.idLong].data)
            )
            return
        }

        when (event.name) {
            "welcome-channel-bind" -> bindWelcomeChannel(event, guild)
            "welcome-channel-unbind" -> unbindWelcomeChannel(event, guild)
            "bye-channel-bind" -> bindByeChannel(event, guild)
            "bye-channel-unbind" -> unbindByeChannel(event, guild)
        }
    }

    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val messageTemplate = messageTemplateRef.get()
        val guild = event.guild
        val setting = jsonGuildManager[guild.idLong].data
        val channelId = setting.welcomeChannelId
        if (channelId == 0L) return

        val channel = guild.getTextChannelById(channelId) ?: return
        val substitutor = WelcomeByeGuildSubstitutorFactory.forMemberEvent(
            guild = guild,
            setting = setting,
            user = event.user,
            member = event.member,
            eventType = "join",
        )

        channel.sendMessage(
            messageTemplate.buildCreate(WelcomeByeGuildMessageKeys.WELCOME, guild.locale, substitutor).build()
        ).queue()
    }

    fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val messageTemplate = messageTemplateRef.get()
        val guild = event.guild
        val setting = jsonGuildManager[guild.idLong].data
        val channelId = setting.byeChannelId
        if (channelId == 0L) return

        val channel = guild.getTextChannelById(channelId) ?: return
        val substitutor = WelcomeByeGuildSubstitutorFactory.forMemberEvent(
            guild = guild,
            setting = setting,
            user = event.user,
            member = null,
            eventType = "leave",
        )

        channel.sendMessage(
            messageTemplate.buildCreate(WelcomeByeGuildMessageKeys.BYE, guild.locale, substitutor).build()
        ).queue()
    }

    private fun bindWelcomeChannel(event: SlashCommandInteractionEvent, guild: Guild) {
        val channel = resolveTargetTextChannel(event) ?: run {
            val setting = jsonGuildManager[guild.idLong].data
            reply(
                event,
                WelcomeByeGuildMessageKeys.INVALID_CHANNEL,
                WelcomeByeGuildSubstitutorFactory.forCommand(event, guild, setting)
            )
            return
        }

        val dataManager = jsonGuildManager[guild.idLong]
        val setting = dataManager.data

        if (setting.welcomeChannelId == channel.idLong) {
            reply(
                event,
                WelcomeByeGuildMessageKeys.WELCOME_BIND_ALREADY,
                WelcomeByeGuildSubstitutorFactory.forCommand(
                    event = event,
                    guild = guild,
                    setting = setting,
                    selectedChannel = channel,
                )
            )
            return
        }

        setting.welcomeChannelId = channel.idLong
        dataManager.save()

        reply(
            event,
            WelcomeByeGuildMessageKeys.WELCOME_BIND_SUCCESS,
            WelcomeByeGuildSubstitutorFactory.forCommand(
                event = event,
                guild = guild,
                setting = setting,
                selectedChannel = channel,
            )
        )
    }

    private fun unbindWelcomeChannel(event: SlashCommandInteractionEvent, guild: Guild) {
        val dataManager = jsonGuildManager[guild.idLong]
        val setting = dataManager.data

        val oldChannelId = setting.welcomeChannelId
        if (oldChannelId == 0L) {
            reply(
                event,
                WelcomeByeGuildMessageKeys.WELCOME_UNBIND_EMPTY,
                WelcomeByeGuildSubstitutorFactory.forCommand(
                    event = event,
                    guild = guild,
                    setting = setting,
                )
            )
            return
        }

        setting.welcomeChannelId = 0L
        dataManager.save()

        reply(
            event,
            WelcomeByeGuildMessageKeys.WELCOME_UNBIND_SUCCESS,
            WelcomeByeGuildSubstitutorFactory.forCommand(
                event = event,
                guild = guild,
                setting = setting,
                oldChannelId = oldChannelId,
            )
        )
    }

    private fun bindByeChannel(event: SlashCommandInteractionEvent, guild: Guild) {
        val channel = resolveTargetTextChannel(event) ?: run {
            val setting = jsonGuildManager[guild.idLong].data
            reply(
                event,
                WelcomeByeGuildMessageKeys.INVALID_CHANNEL,
                WelcomeByeGuildSubstitutorFactory.forCommand(event, guild, setting)
            )
            return
        }

        val dataManager = jsonGuildManager[guild.idLong]
        val setting = dataManager.data

        if (setting.byeChannelId == channel.idLong) {
            reply(
                event,
                WelcomeByeGuildMessageKeys.BYE_BIND_ALREADY,
                WelcomeByeGuildSubstitutorFactory.forCommand(
                    event = event,
                    guild = guild,
                    setting = setting,
                    selectedChannel = channel,
                )
            )
            return
        }

        setting.byeChannelId = channel.idLong
        dataManager.save()

        reply(
            event,
            WelcomeByeGuildMessageKeys.BYE_BIND_SUCCESS,
            WelcomeByeGuildSubstitutorFactory.forCommand(
                event = event,
                guild = guild,
                setting = setting,
                selectedChannel = channel,
            )
        )
    }

    private fun unbindByeChannel(event: SlashCommandInteractionEvent, guild: Guild) {
        val dataManager = jsonGuildManager[guild.idLong]
        val setting = dataManager.data

        val oldChannelId = setting.byeChannelId
        if (oldChannelId == 0L) {
            reply(
                event,
                WelcomeByeGuildMessageKeys.BYE_UNBIND_EMPTY,
                WelcomeByeGuildSubstitutorFactory.forCommand(
                    event = event,
                    guild = guild,
                    setting = setting,
                )
            )
            return
        }

        setting.byeChannelId = 0L
        dataManager.save()

        reply(
            event,
            WelcomeByeGuildMessageKeys.BYE_UNBIND_SUCCESS,
            WelcomeByeGuildSubstitutorFactory.forCommand(
                event = event,
                guild = guild,
                setting = setting,
                oldChannelId = oldChannelId,
            )
        )
    }

    private fun hasAdminPermission(event: SlashCommandInteractionEvent): Boolean {
        return event.member?.hasPermission(Permission.ADMINISTRATOR) == true
    }

    private fun resolveTargetTextChannel(event: SlashCommandInteractionEvent): TextChannel? {
        val optionChannel = event.getOption("channel")?.asChannel
        if (optionChannel != null) {
            return if (optionChannel.type == ChannelType.TEXT) optionChannel.asTextChannel() else null
        }

        val currentChannel = event.guildChannel
        return if (currentChannel.type == ChannelType.TEXT) currentChannel.asTextChannel() else null
    }

    private fun reply(
        event: SlashCommandInteractionEvent,
        messageKey: String,
        substitutor: Substitutor? = null,
    ) {
        val messageTemplate = messageTemplateRef.get()
        val locale = event.guild?.locale ?: event.userLocale
        val editData: MessageEditData = messageTemplate.buildEdit(messageKey, locale, substitutor).build()

        if (event.isAcknowledged) {
            event.hook.editOriginal(editData).queue()
            return
        }

        event.deferReply(true).queue { hook ->
            hook.editOriginal(editData).queue()
        }
    }
}
