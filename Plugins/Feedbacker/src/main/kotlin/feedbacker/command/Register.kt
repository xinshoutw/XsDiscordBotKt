package tw.xinshou.discord.plugin.feedbacker.command

import core.command.CommandHandler
import core.command.slashCommand
import core.i18n.Localizer
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.discord.plugin.feedbacker.Feedbacker

private object Keys {
    const val BASE = "feedbacker"
    const val FEEDBACKER_NAME = "$BASE.name"
    const val FEEDBACKER_DESC = "$BASE.description"

    private const val FEEDBACKER_OPTIONS = "$BASE.options"
    const val FEEDBACKER_OPT_MEMBER_NAME = "$FEEDBACKER_OPTIONS.member.name"
    const val FEEDBACKER_OPT_MEMBER_DESC = "$FEEDBACKER_OPTIONS.member.description"
    const val FEEDBACKER_OPT_SUBMIT_CHANNEL_NAME = "$FEEDBACKER_OPTIONS.submitChannel.name"
    const val FEEDBACKER_OPT_SUBMIT_CHANNEL_DESC = "$FEEDBACKER_OPTIONS.submitChannel.description"
}

internal fun guildCommands(localizer: Localizer): List<CommandHandler> = listOf(
    slashCommand(
        data = Commands.slash("feedbacker", "Create a feedback form for a member")
            .setNameLocalizations(localizer[Keys.FEEDBACKER_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.FEEDBACKER_DESC].toMap())
            .addOptions(
                OptionData(OptionType.USER, "member", "Specify the member", true)
                    .setNameLocalizations(localizer[Keys.FEEDBACKER_OPT_MEMBER_NAME].toMap())
                    .setDescriptionLocalizations(localizer[Keys.FEEDBACKER_OPT_MEMBER_DESC].toMap()),
                OptionData(OptionType.CHANNEL, "submit-channel", "Specify the submit channel", true)
                    .setChannelTypes(ChannelType.TEXT)
                    .setNameLocalizations(localizer[Keys.FEEDBACKER_OPT_SUBMIT_CHANNEL_NAME].toMap())
                    .setDescriptionLocalizations(localizer[Keys.FEEDBACKER_OPT_SUBMIT_CHANNEL_DESC].toMap()),
            ),
    ) { event -> Feedbacker.onSlashCommandInteraction(event) }
)
