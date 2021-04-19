package org.bestever.bebot;

public class BotCommand {
    final String commandSpec;
    final AccountType minimalRole;
    final BotCommandMethod method;

    public BotCommand(String commandSpec, AccountType minimalRole, BotCommandMethod method) {
        this.commandSpec = commandSpec;
        this.minimalRole = minimalRole;
        this.method = method;
    }
}
