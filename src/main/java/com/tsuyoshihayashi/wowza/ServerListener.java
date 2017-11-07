package com.tsuyoshihayashi.wowza;

import com.wowza.wms.server.IServer;
import com.wowza.wms.server.ServerNotifyBase;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Alexey Donov
 */
public class ServerListener extends ServerNotifyBase {
    @Nullable
    private ScheduledFuture fileFuture = null;

    @Override
    public void onServerInit(IServer server) {
        // TODO: Schedule file tracking future
    }

    @Override
    public void onServerShutdownStart(IServer server) {
        Optional.ofNullable(fileFuture).ifPresent(future -> future.cancel(true));
    }
}
