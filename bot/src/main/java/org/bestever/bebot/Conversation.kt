package org.bestever.bebot

import com.mewna.catnip.entity.channel.MessageChannel
import com.mewna.catnip.entity.guild.Member
import org.naturalcli.Command
import org.naturalcli.ExecutionException
import org.naturalcli.NaturalCLI
import org.naturalcli.ParseResult

abstract class Conversation(protected val context: Bot) {
    private val nodes = mutableListOf(Node())
    private val nullResult = CurrentResult(
        BotCommand(".", AccountType.NONE) { _, _ -> },
        ParseResult(emptyArray(), BooleanArray(0))
    )
    private var currentResult = nullResult

    abstract fun defineTree()

    private val compiledNodes by lazy {
        defineTree()
        nodes
            .flatMap {
                it.patterns.map { pattern ->
                    BotCommand(
                        pattern,
                        it.minimalRole,
                        it.implementedBy
                    )
                }
            }
            .map { command ->
                Command(
                    command.commandSpec,
                    command.commandSpec
                ) {
                    currentResult = CurrentResult(command, it)
                }
            }
            .toSet()
    }

    protected operator fun String.unaryPlus(): Node {
        val currentNode = nodes.last()
        currentNode.addPattern(this)
        return currentNode
    }

    protected operator fun String.invoke(function: Node.() -> Unit): String {
        function.invoke(nodes.last())
        nodes.add(Node())
        return this
    }

    fun react(
        member: Member,
        role: AccountType,
        channel: MessageChannel,
        message: String
    ) {
        currentResult = nullResult
        try {
            NaturalCLI(compiledNodes).execute(message)
        } catch(e: ExecutionException) {
            // TODO: say and log the exception
            return
        }
        if (currentResult != nullResult) {
            val nick = member.nick() ?: member.asMention()
            val commandContext = BotContext(channel, member, nick, role)
            currentResult.command.method.invoke(commandContext, currentResult.parseResult)
            // TODO: answer in private
            // TODO: provide some conversation context object to BotCommandMethod
        }
    }

    private data class CurrentResult(
        val command: BotCommand,
        val parseResult: ParseResult
    )

    class Node() {
        val patterns = ArrayList<String>()
        lateinit var implementedBy: BotCommandMethod
        var minimalRole: AccountType = AccountType.NONE

        fun addPattern(pattern: String) {
            patterns.add(pattern)
        }

    }
}