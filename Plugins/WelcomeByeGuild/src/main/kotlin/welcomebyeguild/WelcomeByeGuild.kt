package tw.xinshou.discord.plugin.welcomebyeguild

import com.squareup.moshi.JsonAdapter
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
import tw.xinshou.discord.core.builtin.messagecreator.v2.MessageCreator
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.core.builtin.placeholder.Substitutor
import tw.xinshou.discord.core.json.JsonFileManager
import tw.xinshou.discord.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.discord.core.json.JsonGuildFileManager
import tw.xinshou.discord.plugin.welcomebyeguild.Event.pluginDirectory
import java.io.File

internal object WelcomeByeGuild {
    private val defaultLocale: DiscordLocale = DiscordLocale.CHINESE_TAIWAN

    private val jsonAdapter: JsonAdapter<GuildSetting> = JsonFileManager.moshi.adapterReified<GuildSetting>()
    private val jsonGuildManager = JsonGuildFileManager(
        dataDirectory = File(pluginDirectory, "data"),
        adapter = jsonAdapter,
        defaultInstance = GuildSetting()
    )

    private lateinit var messageCreator: MessageCreator

    internal fun load() {
        messageCreator = MessageCreator(
            pluginDirFile = pluginDirectory,
            defaultLocale = defaultLocale,
        )
    }

    internal fun reload() {
        load()
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        jsonGuildManager.removeAndSave(event.guild.idLong)
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            reply(event, WelcomeByeGuildMessageKeys.GUILD_ONLY)
            return
        }

        val setting = jsonGuildManager[guild.idLong].data
        val commandSubstitutor = WelcomeByeGuildSubstitutorFactory.forCommand(event, guild, setting)

        if (!hasAdminPermission(event)) {
            reply(event, WelcomeByeGuildMessageKeys.NO_PERMISSION, commandSubstitutor)
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
        val guild = event.guild
        val setting = jsonGuildManager.mapper[guild.idLong]?.data ?: return
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
            messageCreator.getCreateBuilder(WelcomeByeGuildMessageKeys.WELCOME, guild.locale, substitutor).build()
        ).queue()
    }

    fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val guild = event.guild
        val setting = jsonGuildManager.mapper[guild.idLong]?.data ?: return
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
            messageCreator.getCreateBuilder(WelcomeByeGuildMessageKeys.BYE, guild.locale, substitutor).build()
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

        val oldChannelId = setting.welcomeChannelId
        if (oldChannelId == 0L) {
            reply(
                event,
                WelcomeByeGuildMessageKeys.WELCOME_UNBIND_EMPTY,
                WelcomeByeGuildSubstitutorFactory.forCommand(
                    event = event,
                    guild = guild,
                    setting = setting,
                    selectedChannel = channel,
                )
            )
            return
        }

        if (oldChannelId != channel.idLong) {
            reply(
                event,
                WelcomeByeGuildMessageKeys.WELCOME_UNBIND_NOT_BOUND,
                WelcomeByeGuildSubstitutorFactory.forCommand(
                    event = event,
                    guild = guild,
                    setting = setting,
                    selectedChannel = channel,
                    oldChannelId = oldChannelId,
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
                selectedChannel = channel,
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

        val oldChannelId = setting.byeChannelId
        if (oldChannelId == 0L) {
            reply(
                event,
                WelcomeByeGuildMessageKeys.BYE_UNBIND_EMPTY,
                WelcomeByeGuildSubstitutorFactory.forCommand(
                    event = event,
                    guild = guild,
                    setting = setting,
                    selectedChannel = channel,
                )
            )
            return
        }

        if (oldChannelId != channel.idLong) {
            reply(
                event,
                WelcomeByeGuildMessageKeys.BYE_UNBIND_NOT_BOUND,
                WelcomeByeGuildSubstitutorFactory.forCommand(
                    event = event,
                    guild = guild,
                    setting = setting,
                    selectedChannel = channel,
                    oldChannelId = oldChannelId,
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
                selectedChannel = channel,
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
        substitutor: Substitutor = Placeholder.globalSubstitutor,
    ) {
        val editData: MessageEditData = messageCreator.getEditBuilder(messageKey, event.userLocale, substitutor).build()

        if (event.isAcknowledged) {
            event.hook.editOriginal(editData).queue()
            return
        }

        event.deferReply(true).queue { hook ->
            hook.editOriginal(editData).queue()
        }
    }
}
