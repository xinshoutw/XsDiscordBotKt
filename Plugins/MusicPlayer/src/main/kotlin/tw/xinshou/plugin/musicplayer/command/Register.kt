package tw.xinshou.plugin.musicplayer.command

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.plugin.musicplayer.command.lang.CmdLocalizations

/**
 * 取得並建構公會特定的指令配置陣列。
 * 每個指令都針對音樂播放功能量身定做，支援YouTube動態搜索、SoundCloud、Spotify等多種音源。
 *
 * @return Array<CommandData> 配置了本地化、權限和自動完成功能的公會指令集合。
 */
internal val guildCommands: Array<CommandData>
    get() = arrayOf(
        // 加入語音頻道指令
        Commands.slash("join", "Make the bot join your voice channel")
            .setNameLocalizations(CmdLocalizations.join.name)
            .setDescriptionLocalizations(CmdLocalizations.join.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

        // 離開語音頻道指令
        Commands.slash("disconnect", "Make the bot leave the voice channel")
            .setNameLocalizations(CmdLocalizations.disconnect.name)
            .setDescriptionLocalizations(CmdLocalizations.disconnect.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

        Commands.slash("play", "Play music")
            .addOptions(
                OptionData(OptionType.STRING, "query", "Song name, artist, or URL", true)
                    .setNameLocalizations(CmdLocalizations.play.options.query.name)
                    .setDescriptionLocalizations(CmdLocalizations.play.options.query.description)
                    .setMaxLength(500)
                    .setRequired(true)
                    .setAutoComplete(true)
            )
            .setNameLocalizations(CmdLocalizations.play.name)
            .setDescriptionLocalizations(CmdLocalizations.play.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

        // 暫停播放指令
        Commands.slash("pause", "Pause the currently playing music")
            .setNameLocalizations(CmdLocalizations.pause.name)
            .setDescriptionLocalizations(CmdLocalizations.pause.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

        // 恢復播放指令
        Commands.slash("resume", "Resume the paused music")
            .setNameLocalizations(CmdLocalizations.resume.name)
            .setDescriptionLocalizations(CmdLocalizations.resume.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

        // 停止播放指令
        Commands.slash("stop", "Stop playing and clear the playlist")
            .setNameLocalizations(CmdLocalizations.stop.name)
            .setDescriptionLocalizations(CmdLocalizations.stop.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

        // 跳過歌曲指令（支援跳過多首）
        Commands.slash("skip", "Skip the currently playing song")
            .addOptions(
                OptionData(OptionType.INTEGER, "count", "Number of songs to skip (default is 1)", false)
                    .setNameLocalizations(CmdLocalizations.skip.options.count.name)
                    .setDescriptionLocalizations(CmdLocalizations.skip.options.count.description)
                    .setMinValue(1L)
                    .setMaxValue(50L) // 限制最多跳過50首
            )
            .setNameLocalizations(CmdLocalizations.skip.name)
            .setDescriptionLocalizations(CmdLocalizations.skip.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

        // 音量控制指令
        Commands.slash("volume", "Set the player volume")
            .addOptions(
                OptionData(OptionType.INTEGER, "level", "Volume level (0-100)", true)
                    .setNameLocalizations(CmdLocalizations.volume.options.level.name)
                    .setDescriptionLocalizations(CmdLocalizations.volume.options.level.description)
                    .setMinValue(0L)
                    .setMaxValue(100L)
                    .setRequired(true)
                    .setAutoComplete(true)
            )
            .setNameLocalizations(CmdLocalizations.volume.name)
            .setDescriptionLocalizations(CmdLocalizations.volume.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

        // 查看播放清單指令
        Commands.slash("queue", "View the current playlist")
            .setNameLocalizations(CmdLocalizations.queue.name)
            .setDescriptionLocalizations(CmdLocalizations.queue.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

        // 查看正在播放指令
        Commands.slash("now-playing", "View detailed information of the currently playing song")
            .setNameLocalizations(CmdLocalizations.nowPlaying.name)
            .setDescriptionLocalizations(CmdLocalizations.nowPlaying.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
    )