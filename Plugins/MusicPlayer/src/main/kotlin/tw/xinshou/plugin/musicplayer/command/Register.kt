package tw.xinshou.plugin.musicplayer.command

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.loader.localizations.StringLocalizer


internal val commandStringSet: Set<String> = setOf(
    "join",
    "disconnect",
    "play",
    "pause",
    "resume",
    "stop",
    "skip",
    "volume",
    "queue",
    "shuffle",
    "now-playing"
)

private object Keys {
    const val JOIN = "join"
    const val JOIN_NAME = "$JOIN.name"
    const val JOIN_DESC = "$JOIN.description"

    const val DISCONNECT = "disconnect"
    const val DISCONNECT_NAME = "$DISCONNECT.name"
    const val DISCONNECT_DESC = "$DISCONNECT.description"

    const val PLAY = "play"
    const val PLAY_NAME = "$PLAY.name"
    const val PLAY_DESC = "$PLAY.description"
    const val PLAY_OPT_QUERY_NAME = "$PLAY.options.query.name"
    const val PLAY_OPT_QUERY_DESC = "$PLAY.options.query.description"

    const val PAUSE = "pause"
    const val PAUSE_NAME = "$PAUSE.name"
    const val PAUSE_DESC = "$PAUSE.description"

    const val RESUME = "resume"
    const val RESUME_NAME = "$RESUME.name"
    const val RESUME_DESC = "$RESUME.description"

    const val STOP = "stop"
    const val STOP_NAME = "$STOP.name"
    const val STOP_DESC = "$STOP.description"

    const val SKIP = "skip"
    const val SKIP_NAME = "$SKIP.name"
    const val SKIP_DESC = "$SKIP.description"
    const val SKIP_OPT_COUNT_NAME = "$SKIP.options.count.name"
    const val SKIP_OPT_COUNT_DESC = "$SKIP.options.count.description"

    const val VOLUME = "volume"
    const val VOLUME_NAME = "$VOLUME.name"
    const val VOLUME_DESC = "$VOLUME.description"
    const val VOLUME_OPT_LEVEL_NAME = "$VOLUME.options.level.name"
    const val VOLUME_OPT_LEVEL_DESC = "$VOLUME.options.level.description"

    const val QUEUE = "queue"
    const val QUEUE_NAME = "$QUEUE.name"
    const val QUEUE_DESC = "$QUEUE.description"

    const val SHUFFLE = "shuffle"
    const val SHUFFLE_NAME = "$SHUFFLE.name"
    const val SHUFFLE_DESC = "$SHUFFLE.description"

    // The key is based on the Kotlin property name 'nowPlaying', not the YAML 'now_playing'
    const val NOW_PLAYING = "nowPlaying"
    const val NOW_PLAYING_NAME = "$NOW_PLAYING.name"
    const val NOW_PLAYING_DESC = "$NOW_PLAYING.description"
}

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("join", "Make the bot join your voice channel")
        .setNameLocalizations(localizer.getLocaleData(Keys.JOIN_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.JOIN_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    Commands.slash("disconnect", "Make the bot leave the voice channel")
        .setNameLocalizations(localizer.getLocaleData(Keys.DISCONNECT_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.DISCONNECT_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    Commands.slash("play", "Play music")
        .addOptions(
            OptionData(OptionType.STRING, "query", "Song name, artist, or URL", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.PLAY_OPT_QUERY_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.PLAY_OPT_QUERY_DESC))
                .setMaxLength(500)
                .setAutoComplete(true)
        )
        .setNameLocalizations(localizer.getLocaleData(Keys.PLAY_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.PLAY_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    Commands.slash("pause", "Pause the currently playing music")
        .setNameLocalizations(localizer.getLocaleData(Keys.PAUSE_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.PAUSE_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    Commands.slash("resume", "Resume the paused music")
        .setNameLocalizations(localizer.getLocaleData(Keys.RESUME_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.RESUME_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    Commands.slash("stop", "Stop playing and clear the playlist")
        .setNameLocalizations(localizer.getLocaleData(Keys.STOP_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.STOP_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    Commands.slash("skip", "Skip the currently playing song")
        .addOptions(
            OptionData(OptionType.INTEGER, "count", "Number of songs to skip (default is 1)", false)
                .setNameLocalizations(localizer.getLocaleData(Keys.SKIP_OPT_COUNT_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.SKIP_OPT_COUNT_DESC))
                .setMinValue(1L)
                .setMaxValue(50L)
        )
        .setNameLocalizations(localizer.getLocaleData(Keys.SKIP_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.SKIP_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    Commands.slash("volume", "Set the player volume")
        .addOptions(
            OptionData(OptionType.INTEGER, "level", "Volume level (0-100)", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.VOLUME_OPT_LEVEL_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.VOLUME_OPT_LEVEL_DESC))
                .setMinValue(0L)
                .setMaxValue(100L)
                .setAutoComplete(true)
        )
        .setNameLocalizations(localizer.getLocaleData(Keys.VOLUME_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.VOLUME_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    Commands.slash("queue", "View the current playlist")
        .setNameLocalizations(localizer.getLocaleData(Keys.QUEUE_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.QUEUE_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    Commands.slash("shuffle", "Shuffle the current playlist once")
        .setNameLocalizations(localizer.getLocaleData(Keys.SHUFFLE_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.SHUFFLE_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    Commands.slash("now-playing", "View detailed information of the currently playing song")
        .setNameLocalizations(localizer.getLocaleData(Keys.NOW_PLAYING_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.NOW_PLAYING_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
)