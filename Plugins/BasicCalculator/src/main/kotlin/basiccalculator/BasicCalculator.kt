package tw.xinshou.discord.plugin.basiccalculator

import core.i18n.MessageTemplate
import core.placeholder.Substitutor
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*


internal object BasicCalculator {
    private lateinit var messageTemplate: MessageTemplate

    internal fun init() {
        messageTemplate = MessageTemplate(
            langDir = Event.pluginContext.pluginDirectory.resolve("lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val formula: String = event.getOption("formula")!!.asString.trim().trimEnd('?', '=', ' ')
        val ans: String
        try {
            ans = evaluateExpression(formula)
        } catch (e: IllegalArgumentException) {
            val substitutor = Substitutor().putAll(
                "bc_question" to formula,
                "bc_error" to (e.message ?: "Unknown error"),
            )

            event.hook.editOriginal(
                messageTemplate.buildEdit(
                    "error",
                    event.userLocale,
                    substitutor,
                ).build()
            ).queue()
            return
        }

        val substitutor = Substitutor().putAll(
            "bc_question" to formula,
            "bc_answer" to ans,
            "bc_answer_round" to ans.split('.', limit = 1)[0],
        )

        event.channel.sendMessage(
            messageTemplate.buildCreate(
                "basic-calculate",
                event.userLocale,
                substitutor,
            ).build()
        ).flatMap {
            event.hook.deleteOriginal()
        }.queue()
    }


    private enum class TokenType {
        NUMBER, OPERATOR, LEFT_PAREN, RIGHT_PAREN
    }

    private fun evaluateExpression(expression: String): String {
        fun isOperator(c: Char): Boolean = c in listOf('+', '-', '*', '/', '^')

        fun precedence(c: Char): Int = when (c) {
            '+', '-' -> 1
            '*', '/' -> 2
            '^' -> 3
            else -> 0
        }

        fun infixToPostfix(exp: String): List<String> {
            val output = mutableListOf<String>()
            val stack = Stack<Char>()
            var i = 0
            var prevTokenType: TokenType = TokenType.OPERATOR


            while (i < exp.length) {
                val c = exp[i]

                if (c.isWhitespace()) {
                    i++
                    continue
                }

                if (c.isDigit() || c == '.') {
                    if (prevTokenType == TokenType.NUMBER || prevTokenType == TokenType.RIGHT_PAREN) {
                        while (!stack.isEmpty() && precedence('*') <= precedence(stack.peek())) {
                            output.add(stack.pop().toString())
                        }
                        stack.push('*')
                    }

                    val sb = StringBuilder()
                    while (i < exp.length && (exp[i].isDigit() || exp[i] == '.')) {
                        sb.append(exp[i])
                        i++
                    }
                    output.add(sb.toString())
                    prevTokenType = TokenType.NUMBER
                    continue
                } else if (c == '(') {
                    if (prevTokenType == TokenType.NUMBER || prevTokenType == TokenType.RIGHT_PAREN) {
                        while (!stack.isEmpty() && precedence('*') <= precedence(stack.peek())) {
                            output.add(stack.pop().toString())
                        }
                        stack.push('*')
                    }
                    stack.push(c)
                    prevTokenType = TokenType.LEFT_PAREN
                } else if (c == ')') {
                    while (!stack.isEmpty() && stack.peek() != '(') {
                        output.add(stack.pop().toString())
                    }
                    if (stack.isEmpty()) {
                        throw IllegalArgumentException("Unmatched bracket")
                    }
                    stack.pop()
                    prevTokenType = TokenType.RIGHT_PAREN
                } else if (isOperator(c)) {
                    if (c == '-' && (prevTokenType == TokenType.OPERATOR || prevTokenType == TokenType.LEFT_PAREN)) {
                        output.add("0")
                    }
                    while (!stack.isEmpty() && precedence(c) <= precedence(stack.peek())) {
                        output.add(stack.pop().toString())
                    }
                    stack.push(c)
                    prevTokenType = TokenType.OPERATOR
                } else {
                    throw IllegalArgumentException("Unknown character: $c")
                }
                i++
            }

            while (!stack.isEmpty()) {
                val op = stack.pop()
                if (op == '(' || op == ')') {
                    throw IllegalArgumentException("Unmatched bracket")
                }
                output.add(op.toString())
            }

            return output
        }

        fun evaluatePostfix(postfixTokens: List<String>): String {
            val stack = Stack<BigDecimal>()

            for (token in postfixTokens) {
                when {
                    token.toBigDecimalOrNull() != null -> {
                        stack.push(token.toBigDecimal())
                    }

                    isOperator(token[0]) && token.length == 1 -> {
                        val op = token[0]
                        if (stack.size < 2) {
                            throw IllegalArgumentException("Illegal formula")
                        }
                        val b = stack.pop()
                        val a = stack.pop()
                        val result = when (op) {
                            '+' -> a.add(b)
                            '-' -> a.subtract(b)
                            '*' -> a.multiply(b)
                            '/' -> a.divide(b, 10, RoundingMode.HALF_UP)
                            '^' -> a.pow(b.toInt())
                            else -> throw IllegalArgumentException("Unknown Operator: $op")
                        }
                        stack.push(result)
                    }

                    else -> {
                        throw IllegalArgumentException("Unknown Token: $token")
                    }
                }
            }
            if (stack.size != 1) {
                throw IllegalArgumentException("Illegal formula")
            }


            return stack.pop()
                .setScale(5, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }

        return evaluatePostfix(infixToPostfix(expression))
    }
}
