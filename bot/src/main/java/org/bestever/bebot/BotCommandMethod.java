package org.bestever.bebot;

import org.naturalcli.ParseResult;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface BotCommandMethod {
    void invoke(@Nonnull BotContext context, @Nonnull ParseResult parseResult) throws InputException;
}
