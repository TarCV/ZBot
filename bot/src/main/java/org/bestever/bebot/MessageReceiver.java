package org.bestever.bebot;

public interface MessageReceiver {
    void onMessage(String message);
    void onError(String message);
}
