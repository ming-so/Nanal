package com.android.nanal.interfaces;

import android.content.Context;
import android.os.Bundle;

import java.io.IOException;

public interface CloudNotificationBackplane {
    public boolean open(Context context);
    public boolean subscribeToGroup(String senderId, String account, String groupId)
            throws IOException;
    public void send(String to, String msgId, Bundle data) throws IOException;
    public void close();
}
