package tw.xinshou.discord.plugin.economy

import tw.xinshou.discord.core.placeholder.Substitutor
import tw.xinshou.discord.core.placeholder.withUser
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import tw.xinshou.discord.plugin.economy.Event.pluginConfig
import tw.xinshou.discord.plugin.economy.Event.storageManager


internal enum class Mode {
    Json,
    GoogleSheet,
}

internal object Economy {
    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.idLong ?: return

        if (event.name.startsWith("top-")) {
            handleTopCommands(event, guildId)
            return
        }

        when (event.name) {
            "balance" -> handleBalance(event, guildId)
            "add-money", "remove-money", "set-money", "add-cost", "remove-cost", "set-cost" ->
                handleMoneyAndCostCommands(event, guildId)
        }
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        val guildId = event.guild?.idLong ?: return
        event.deferReply(true).queue {
            handleButtonBalance(event, it, guildId)
        }
    }


    private fun handleTopCommands(event: SlashCommandInteractionEvent, guildId: Long) {
        if (checkPermission(event)) return

        event.hook.editOriginal(
            MessageReplier.replyBoard(
                key = event.name,
                guildId = guildId,
                user = event.user,
                userLocale = event.userLocale
            )
        ).queue()
    }

    private fun handleButtonBalance(event: ButtonInteractionEvent, hook: InteractionHook, guildId: Long) {
        val userData = queryData(guildId, event.user)
        val substitutor = Substitutor().withUser(event.user).putAll(
            "economy_money" to "${userData.data.money}",
            "economy_cost" to "${userData.data.cost}"
        )
        hook.editOriginal(
            MessageReplier.getMessageEditData("balance", event.userLocale, substitutor)
        ).queue()
    }

    private fun handleBalance(event: SlashCommandInteractionEvent, guildId: Long) {
        val targetUser = getTargetUser(event)
        val userData = queryData(guildId, targetUser)
        val substitutor = Substitutor().withUser(targetUser).putAll(
            "economy_money" to "${userData.data.money}",
            "economy_cost" to "${userData.data.cost}"
        )
        event.hook.editOriginal(
            MessageReplier.getMessageEditData("balance", event.userLocale, substitutor)
        ).queue()
    }

    private fun handleMoneyAndCostCommands(event: SlashCommandInteractionEvent, guildId: Long) {
        if (checkPermission(event)) return
        val value: Int = event.getOption("value", 0) { it.asInt }
        if (checkValue(value, event)) return

        val targetUser = getTargetUser(event)
        val userData = queryData(guildId, targetUser)
        when (event.name) {
            "add-money" -> {
                val before = userData.data.money
                userData.addMoney(value)
                saveAndUpdateSubstitutor(guildId, targetUser, userData, "economy_money_before" to "$before")
                reply(event, Substitutor().withUser(targetUser).putAll(
                    "economy_money" to "${userData.data.money}", "economy_cost" to "${userData.data.cost}",
                    "economy_money_before" to "$before"
                ))
                storageManager.sortMoneyBoard()
            }

            "remove-money" -> {
                val beforeMoney = userData.data.money
                val beforeCost = userData.data.cost
                userData.removeMoneyAddCost(value)
                saveData(guildId, userData)
                reply(event, Substitutor().withUser(targetUser).putAll(
                    "economy_money" to "${userData.data.money}", "economy_cost" to "${userData.data.cost}",
                    "economy_money_before" to "$beforeMoney", "economy_cost_before" to "$beforeCost"
                ))
                storageManager.sortMoneyBoard()
                storageManager.sortCostBoard()
            }

            "set-money" -> {
                val before = userData.data.money
                userData.setMoney(value)
                saveData(guildId, userData)
                reply(event, Substitutor().withUser(targetUser).putAll(
                    "economy_money" to "${userData.data.money}", "economy_cost" to "${userData.data.cost}",
                    "economy_money_before" to "$before"
                ))
                storageManager.sortMoneyBoard()
            }

            "add-cost" -> {
                val before = userData.data.cost
                userData.addCost(value)
                saveData(guildId, userData)
                reply(event, Substitutor().withUser(targetUser).putAll(
                    "economy_money" to "${userData.data.money}", "economy_cost" to "${userData.data.cost}",
                    "economy_cost_before" to "$before"
                ))
                storageManager.sortCostBoard()
            }

            "remove-cost" -> {
                val before = userData.data.cost
                userData.removeCost(value)
                saveData(guildId, userData)
                reply(event, Substitutor().withUser(targetUser).putAll(
                    "economy_money" to "${userData.data.money}", "economy_cost" to "${userData.data.cost}",
                    "economy_cost_before" to "$before"
                ))
                storageManager.sortCostBoard()
            }

            "set-cost" -> {
                val before = userData.data.cost
                userData.setCost(value)
                saveData(guildId, userData)
                reply(event, Substitutor().withUser(targetUser).putAll(
                    "economy_money" to "${userData.data.money}", "economy_cost" to "${userData.data.cost}",
                    "economy_cost_before" to "$before"
                ))
                storageManager.sortCostBoard()
            }
        }
    }

    private fun saveAndUpdateSubstitutor(
        guildId: Long, user: User, data: UserData, vararg placeholders: Pair<String, String>
    ) {
        saveData(guildId, data)
    }

    private fun reply(event: SlashCommandInteractionEvent, substitutor: Substitutor) {
        event.hook.editOriginal(
            MessageReplier.getMessageEditData(event.name, event.userLocale, substitutor)
        ).queue()
    }

    private fun checkValue(value: Int, event: SlashCommandInteractionEvent): Boolean {
        if (value >= 0) return false
        event.hook.editOriginal("Bad Value").queue()
        return true
    }

    private fun queryData(guildId: Long, user: User): UserData = storageManager.query(guildId, user)
    private fun saveData(guildId: Long, data: UserData) = storageManager.update(guildId, data)

    private fun getTargetUser(event: SlashCommandInteractionEvent): User {
        return if (pluginConfig.adminId.none { it == event.user.idLong }) event.user
        else event.getOption("member")?.asUser ?: event.user
    }

    private fun checkPermission(event: SlashCommandInteractionEvent): Boolean {
        if (pluginConfig.adminId.none { it == event.user.idLong }) {
            event.hook.editOriginal(
                MessageReplier.getNoPermissionMessageData(event.userLocale)
            ).queue()
            return true
        }
        return false
    }

    enum class Type { Money, Cost }
}
