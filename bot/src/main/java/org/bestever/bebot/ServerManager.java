package org.bestever.bebot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface ServerManager {
    int getMinPort();

    int getMaxPort();

    void removeServerFromLinkedList(Server server);

    Server getServer(int port);

    @Nonnull
    List<Server> getUserServers(String userId);

    @Nullable
    List<Server> getAllServers();

    void addToLinkedList(Server server);
}
