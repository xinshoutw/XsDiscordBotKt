package tw.xinshou.discord.plugin.dynamicvoicechannel

import core.i18n.MessageTemplate
import core.placeholder.Substitutor
import core.placeholder.withCommand
import core.placeholder.withMember
import core.placeholder.withUser
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.plugin.dynamicvoicechannel.Event.pluginDirectory
import tw.xinshou.discord.plugin.dynamicvoicechannel.json.DataContainer
import java.io.File


internal object DynamicVoiceChannel {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    // TODO: MongoDB cache replacement - using in-memory map for now
    private val generatedCache: MutableMap<Long, Long> = mutableMapOf()

    private var messageTemplate = MessageTemplate(
        langDir = File(pluginDirectory, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
    )

    internal fun reload() {
        messageTemplate = MessageTemplate(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )
    }

    fun bind(event: SlashCommandInteractionEvent) {
        val locale = event.userLocale
        val guild = event.guild!!
        val channel = event.getOption("channel", event.channel) { it.asChannel } as VoiceChannel
        val formatName1 = event.getOption("format-name-1") { it.asString }!!
        val formatName2 = event.getOption("format-name-2") { it.asString }!!
        val category = channel.parentCategory

        val substitutor = Substitutor()
            .withUser(event.user)
            .withMember(event.member)
            .withCommand(event)

        if (category == null) {
            event.hook.editOriginal(
                messageTemplate.buildEdit("must-under-category", locale, substitutor).build()
            ).queue()
            return
        }

        JsonManager.addData(guild.idLong, DataContainer(category.idLong, channel.name, formatName1, formatName2))
        event.hook.editOriginal(
            messageTemplate.buildEdit("bind-success", locale, substitutor).build()
        ).queue()
    }

    fun unbind(event: SlashCommandInteractionEvent) {
        val locale = event.userLocale
        val guild = event.guild!!
        val channel = event.getOption("channel", event.channel) { it.asChannel } as VoiceChannel
        val category = channel.parentCategory

        val substitutor = Substitutor()
            .withUser(event.user)
            .withMember(event.member)
            .withCommand(event)

        if (category == null) {
            event.hook.editOriginal(
                messageTemplate.buildEdit("must-under-category", locale, substitutor).build()
            ).queue()
            return
        }

        val status = JsonManager.removeData(guild.idLong, category.idLong, channel.name)

        event.hook.editOriginal(
            messageTemplate.buildEdit(
                if (status) "unbind-fail" else "unbind-success",
                locale,
                substitutor
            ).build()
        ).queue()
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        JsonManager.removeGuild(event.guild.idLong)
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.fullCommandName) {
            "dynamic-voice-channel bind" -> bind(event)
            "dynamic-voice-channel unbind" -> unbind(event)
        }
    }

    fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val channelJoin = event.channelJoined
        val channelLeft = event.channelLeft

        if (channelJoin != null && channelJoin.members.size == 1) {
            val data = JsonManager.getData(channelJoin.parentCategoryIdLong, channelJoin.name) ?: return
            firstJoin(event, channelJoin.asVoiceChannel(), data)
        }

        if (channelLeft != null && channelLeft.members.isEmpty()) {
            if (generatedCache.containsKey(channelLeft.idLong)) {
                channelLeft.delete().queue()
                generatedCache.remove(channelLeft.idLong)
            }
        }
    }

    fun firstJoin(event: GuildVoiceUpdateEvent, channelJoin: VoiceChannel, data: DataContainer) {
        val substitutor = Substitutor()
            .withUser(event.member.user)
            .withMember(event.member)
            .put("dvc@custom_name", event.member.effectiveName.split(" - ").first())

        channelJoin.createCopy().flatMap {
            it.manager.setPosition(0)
        }.flatMap {
            channelJoin.manager.let {
                it.setPosition(1)
                it.setName(substitutor.parse(data.formatName1))
                it.putMemberPermissionOverride(
                    event.member.idLong,
                    listOf(Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS),
                    emptyList<Permission>()
                )
            }
        }.queue()

        generatedCache[channelJoin.idLong] = event.member.idLong
    }
}
