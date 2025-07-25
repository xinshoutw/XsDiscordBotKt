package tw.xinshou.plugin.rentsystem.command.lang

import tw.xinshou.loader.localizations.LocalTemplate
import tw.xinshou.loader.localizations.LocaleData

internal object CmdLocalizations {
    val meter = CommandLd()

    internal class CommandLd {
        val name = LocaleData()
        val description = LocaleData()
        val options = OptionsLd()

        internal class OptionsLd {
            val public = LocalTemplate.NDLocalData()
            val roomA = LocalTemplate.NDLocalData()
            val roomB = LocalTemplate.NDLocalData()
            val roomC = LocalTemplate.NDLocalData()
        }
    }
}