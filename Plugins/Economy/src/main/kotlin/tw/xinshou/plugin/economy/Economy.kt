package tw.xinshou.plugin.economy

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import tw.xinshou.loader.builtin.placeholder.Placeholder
import tw.xinshou.loader.builtin.placeholder.Substitutor
import tw.xinshou.plugin.economy.Event.config
import tw.xinshou.plugin.economy.Event.storageManager

internal object Economy {
    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name.startsWith("top-")) {
            handleTopCommands(event)
            return
        }

        when (event.name) {
            "balance" -> handleBalance(event)
            "add-money", "remove-money", "set-money", "add-cost", "remove-cost", "set-cost" ->
                handleMoneyAndCostCommands(event)
        }
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        event.deferReply(true).queue {
            handleButtonBalance(event, it)
        }
    }


    private fun handleTopCommands(event: SlashCommandInteractionEvent) {
        if (checkPermission(event)) return

        event.hook.editOriginal(
            MessageReplier.replyBoard(
                key = event.name,
                user = event.user,
                userLocale = event.userLocale
            )
        ).queue()
    }

    private fun handleButtonBalance(event: ButtonInteractionEvent, hook: InteractionHook) {
        updatePapi(event.user)
        hook.editOriginal(
            MessageReplier.getMessageEditData("balance", event.userLocale, Placeholder.get(event.user))
        ).queue()
    }

    private fun handleBalance(
        event: SlashCommandInteractionEvent
    ) {
        val targetUser = getTargetUser(event)
        updatePapi(targetUser)
        event.hook.editOriginal(
            MessageReplier.getMessageEditData("balance", event.userLocale, Placeholder.get(targetUser))
        ).queue()
    }

    private fun handleMoneyAndCostCommands(
        event: SlashCommandInteractionEvent
    ) {
        if (checkPermission(event)) return
        val value: Int = event.getOption("value", 0) { it.asInt }
        if (checkValue(value, event)) return

        val targetUser = getTargetUser(event)
        val userData = queryData(targetUser)
        when (event.name) {
            "add-money" -> {
                val before = userData.data.money
                userData.addMoney(value)
                saveAndUpdate(targetUser, userData, "economy_money_before" to "$before")
                reply(event, Placeholder.get(targetUser))
                storageManager.sortMoneyBoard()
            }

            "remove-money" -> {
                val beforeMoney = userData.data.money
                val beforeCost = userData.data.cost
                userData.removeMoneyAddCost(value)
                saveAndUpdate(
                    targetUser, userData,
                    "economy_money_before" to "$beforeMoney",
                    "economy_cost_before" to "$beforeCost"
                )
                reply(event, Placeholder.get(targetUser))
                storageManager.sortMoneyBoard()
                storageManager.sortCostBoard()
            }

            "set-money" -> {
                val before = userData.data.money
                userData.setMoney(value)
                saveAndUpdate(targetUser, userData, "economy_money_before" to "$before")
                reply(event, Placeholder.get(targetUser))
                storageManager.sortMoneyBoard()
            }

            "add-cost" -> {
                val before = userData.data.cost
                userData.addCost(value)
                saveAndUpdate(targetUser, userData, "economy_cost_before" to "$before")
                reply(event, Placeholder.get(targetUser))
                storageManager.sortCostBoard()
            }

            "remove-cost" -> {
                val before = userData.data.cost
                userData.removeCost(value)
                saveAndUpdate(targetUser, userData, "economy_cost_before" to "$before")
                reply(event, Placeholder.get(targetUser))
                storageManager.sortCostBoard()
            }

            "set-cost" -> {
                val before = userData.data.cost
                userData.setCost(value)
                saveAndUpdate(targetUser, userData, "economy_cost_before" to "$before")
                reply(event, Placeholder.get(targetUser))
                storageManager.sortCostBoard()
            }
        }
    }

    private fun updatePapi(user: User) {
        val userData = queryData(user)
        Placeholder.update(
            user, hashMapOf(
                "economy_money" to "${userData.data.money}", "economy_cost" to "${userData.data.cost}"
            )
        )
    }

    private fun updatePapi(user: User, userData: UserData, map: Map<String, String> = emptyMap()) {
        Placeholder.update(
            user, hashMapOf(
                "economy_money" to "${userData.data.money}", "economy_cost" to "${userData.data.cost}"
            ).apply { putAll(map) })
    }


    private fun saveAndUpdate(
        user: User,
        data: UserData,
        vararg placeholders: Pair<String, String>
    ) {
        saveData(data)
        updatePapi(user, data, placeholders.toMap())
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

    private fun queryData(user: User): UserData = storageManager.query(user)

    private fun saveData(data: UserData) = storageManager.update(data)

    private fun getTargetUser(event: SlashCommandInteractionEvent): User {
        return if (config.adminId.none { it == event.user.idLong }) event.user
        else event.getOption("member")?.asUser ?: event.user
    }

    private fun checkPermission(event: SlashCommandInteractionEvent): Boolean {
        if (config.adminId.none { it == event.user.idLong }) {
            event.hook.editOriginal(
                MessageReplier.getNoPermissionMessageData(event.userLocale)
            ).queue()
            return true
        }
        return false
    }

    enum class Type {
        Money, Cost
    }
}