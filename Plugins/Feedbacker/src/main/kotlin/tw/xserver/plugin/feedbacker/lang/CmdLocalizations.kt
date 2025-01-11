package tw.xserver.plugin.feedbacker.lang

import tw.xserver.loader.localizations.LocalTemplate
import tw.xserver.loader.localizations.LocaleData

internal object CmdLocalizations {
    val feedbacker = CommandWithMemberLd()

    internal class CommandWithMemberLd {
        val options = OptionsLd()
        val name = LocaleData()
        val description = LocaleData()

        internal class OptionsLd {
            val member = LocalTemplate.NDLocalData()
        }
    }
}