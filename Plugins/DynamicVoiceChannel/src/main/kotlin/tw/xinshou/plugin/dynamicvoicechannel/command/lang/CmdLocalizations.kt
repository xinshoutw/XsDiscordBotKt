package tw.xinshou.plugin.dynamicvoicechannel.command.lang

import tw.xinshou.loader.localizations.LocaleData

internal object CmdLocalizations {
    val dynamicvc = CommandWithMemberLd()

    class CommandWithMemberLd {
        val name = LocaleData()
        val description = LocaleData()
        val subcommands = SubCommandsLd()

        class SubCommandsLd {
            val bind = BindLd()
            val unbind = UnbindLd()

            class BindLd {
                val name = LocaleData()
                val description = LocaleData()
                val options = OptionsLd()

                class OptionsLd {
                    val channel = LocaleData()
                    val formatName1 = LocaleData()
                    val formatName2 = LocaleData()
                }
            }

            class UnbindLd {
                val name = LocaleData()
                val description = LocaleData()
                val options = OptionsLd()

                class OptionsLd {
                    val channel = LocaleData()
                }
            }
        }
    }
}