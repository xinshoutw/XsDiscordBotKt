package tw.xinshou.plugin.basiccalculator.command.lang

import tw.xinshou.loader.localizations.LocalTemplate
import tw.xinshou.loader.localizations.LocaleData

internal object CmdLocalizations {
    val basicCalculate = CommandWithMemberLd()

    class CommandWithMemberLd {
        val name = LocaleData()
        val description = LocaleData()
        val options = OptionsLd()

        class OptionsLd {
            val formula = LocalTemplate.NDLocalData()
        }
    }
}