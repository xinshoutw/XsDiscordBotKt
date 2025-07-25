package tw.xinshou.plugin.musicplayer.command.lang

import tw.xinshou.loader.localizations.LocalTemplate
import tw.xinshou.loader.localizations.LocaleData

internal object CmdLocalizations {
    val join = LocalTemplate.NDLocalData()
    val play = CommandPlay()
    val pause = LocalTemplate.NDLocalData()
    val resume = LocalTemplate.NDLocalData()
    val stop = LocalTemplate.NDLocalData()
    val disconnect = LocalTemplate.NDLocalData()

    val skip = CommandSkip()
    val volume = CommandVolume()
    val queue = LocalTemplate.NDLocalData()
    val shuffle = LocalTemplate.NDLocalData()

    val nowPlaying = LocalTemplate.NDLocalData()

    internal class CommandPlay {
        val options = OptionsLd()
        val name = LocaleData()
        val description = LocaleData()

        internal class OptionsLd {
            val query = LocalTemplate.NDLocalData()
        }
    }

    internal class CommandSkip {
        val options = OptionsLd()
        val name = LocaleData()
        val description = LocaleData()

        internal class OptionsLd {
            val count = LocalTemplate.NDLocalData()
        }
    }

    internal class CommandVolume {
        val options = OptionsLd()
        val name = LocaleData()
        val description = LocaleData()

        internal class OptionsLd {
            val level = LocalTemplate.NDLocalData()
        }
    }
}
