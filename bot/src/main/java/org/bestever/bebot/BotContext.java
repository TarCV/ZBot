package org.bestever.bebot;

import com.mewna.catnip.entity.channel.MessageChannel;
import com.mewna.catnip.entity.guild.Member;

public class BotContext {
    public final MessageChannel channel;
    public final Member member;
    public final String nickname;
    public final AccountType level;

    public BotContext(MessageChannel channel, Member member, String nickname, AccountType level) {
        this.channel = channel;
        this.member = member;
        this.nickname = nickname;
        this.level = level;
    }
}
