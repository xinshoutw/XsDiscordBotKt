package tw.xinshou.discord.plugin.musicplayer.command

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.slashCommand
import tw.xinshou.discord.core.i18n.Localizer
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.discord.plugin.musicplayer.MusicPlayer

private object Keys {
    const val JOIN = "join"; const val JOIN_NAME = "$JOIN.name"; const val JOIN_DESC = "$JOIN.description"
    const val DISCONNECT = "disconnect"; const val DISCONNECT_NAME = "$DISCONNECT.name"; const val DISCONNECT_DESC = "$DISCONNECT.description"
    const val PLAY = "play"; const val PLAY_NAME = "$PLAY.name"; const val PLAY_DESC = "$PLAY.description"
    const val PLAY_OPT_QUERY_NAME = "$PLAY.options.query.name"; const val PLAY_OPT_QUERY_DESC = "$PLAY.options.query.description"
    const val PAUSE = "pause"; const val PAUSE_NAME = "$PAUSE.name"; const val PAUSE_DESC = "$PAUSE.description"
    const val RESUME = "resume"; const val RESUME_NAME = "$RESUME.name"; const val RESUME_DESC = "$RESUME.description"
    const val STOP = "stop"; const val STOP_NAME = "$STOP.name"; const val STOP_DESC = "$STOP.description"
    const val SKIP = "skip"; const val SKIP_NAME = "$SKIP.name"; const val SKIP_DESC = "$SKIP.description"
    const val SKIP_OPT_COUNT_NAME = "$SKIP.options.count.name"; const val SKIP_OPT_COUNT_DESC = "$SKIP.options.count.description"
    const val VOLUME = "volume"; const val VOLUME_NAME = "$VOLUME.name"; const val VOLUME_DESC = "$VOLUME.description"
    const val VOLUME_OPT_LEVEL_NAME = "$VOLUME.options.level.name"; const val VOLUME_OPT_LEVEL_DESC = "$VOLUME.options.level.description"
    const val QUEUE = "queue"; const val QUEUE_NAME = "$QUEUE.name"; const val QUEUE_DESC = "$QUEUE.description"
    const val SHUFFLE = "shuffle"; const val SHUFFLE_NAME = "$SHUFFLE.name"; const val SHUFFLE_DESC = "$SHUFFLE.description"
    const val NOW_PLAYING = "nowPlaying"; const val NOW_PLAYING_NAME = "$NOW_PLAYING.name"; const val NOW_PLAYING_DESC = "$NOW_PLAYING.description"
}

internal fun guildCommands(localizer: Localizer): List<CommandHandler> {
    val handler: suspend (net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent) -> Unit =
        { event -> MusicPlayer.onSlashCommandInteraction(event) }

    return listOf(
        slashCommand(data = Commands.slash("join", "Join voice channel").setNameLocalizations(localizer[Keys.JOIN_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.JOIN_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
        slashCommand(data = Commands.slash("disconnect", "Leave voice channel").setNameLocalizations(localizer[Keys.DISCONNECT_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.DISCONNECT_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
        slashCommand(data = Commands.slash("play", "Play music").addOptions(OptionData(OptionType.STRING, "query", "Song name or URL", true).setNameLocalizations(localizer[Keys.PLAY_OPT_QUERY_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.PLAY_OPT_QUERY_DESC].toMap()).setMaxLength(500).setAutoComplete(true)).setNameLocalizations(localizer[Keys.PLAY_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.PLAY_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
        slashCommand(data = Commands.slash("pause", "Pause music").setNameLocalizations(localizer[Keys.PAUSE_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.PAUSE_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
        slashCommand(data = Commands.slash("resume", "Resume music").setNameLocalizations(localizer[Keys.RESUME_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.RESUME_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
        slashCommand(data = Commands.slash("stop", "Stop and clear").setNameLocalizations(localizer[Keys.STOP_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.STOP_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
        slashCommand(data = Commands.slash("skip", "Skip song").addOptions(OptionData(OptionType.INTEGER, "count", "Number to skip", false).setNameLocalizations(localizer[Keys.SKIP_OPT_COUNT_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.SKIP_OPT_COUNT_DESC].toMap()).setMinValue(1L).setMaxValue(50L)).setNameLocalizations(localizer[Keys.SKIP_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.SKIP_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
        slashCommand(data = Commands.slash("volume", "Set volume").addOptions(OptionData(OptionType.INTEGER, "level", "Volume 0-100", true).setNameLocalizations(localizer[Keys.VOLUME_OPT_LEVEL_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.VOLUME_OPT_LEVEL_DESC].toMap()).setMinValue(0L).setMaxValue(100L).setAutoComplete(true)).setNameLocalizations(localizer[Keys.VOLUME_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.VOLUME_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
        slashCommand(data = Commands.slash("queue", "View playlist").setNameLocalizations(localizer[Keys.QUEUE_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.QUEUE_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
        slashCommand(data = Commands.slash("shuffle", "Shuffle playlist").setNameLocalizations(localizer[Keys.SHUFFLE_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.SHUFFLE_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
        slashCommand(data = Commands.slash("now-playing", "Now playing info").setNameLocalizations(localizer[Keys.NOW_PLAYING_NAME].toMap()).setDescriptionLocalizations(localizer[Keys.NOW_PLAYING_DESC].toMap()).setDefaultPermissions(DefaultMemberPermissions.ENABLED), execute = handler),
    )
}
