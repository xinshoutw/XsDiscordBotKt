package tw.xinshou.plugin.economy.command.lang

import tw.xinshou.loader.localizations.LocalTemplate
import tw.xinshou.loader.localizations.LocaleData
import tw.xinshou.loader.localizations.LocalTemplate as LocalTemplate1

internal object CmdLocalizations {
    val balance = CommandWithMemberLd()
    val topMoney = LocalTemplate.NDLocalData()
    val topCost = LocalTemplate.NDLocalData()
    val addMoney = CommandWithMemberValueLd()
    val removeMoney = CommandWithMemberValueLd()
    val setMoney = CommandWithMemberValueLd()
    val addCost = CommandWithMemberValueLd()
    val removeCost = CommandWithMemberValueLd()
    val setCost = CommandWithMemberValueLd()

    internal class CommandWithMemberLd {
        val options = OptionsLd()
        val name = LocaleData()
        val description = LocaleData()

        internal class OptionsLd {
            val member = LocalTemplate1.NDLocalData()
        }
    }

    internal class CommandWithMemberValueLd {
        val options = OptionsLd()
        val name = LocaleData()
        val description = LocaleData()

        internal class OptionsLd {
            val member = LocalTemplate1.NDLocalData()
            val value = LocalTemplate1.NDLocalData()
        }
    }
}