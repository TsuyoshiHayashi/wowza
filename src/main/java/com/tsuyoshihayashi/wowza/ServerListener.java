package com.tsuyoshihayashi.wowza;

import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.server.IServer;
import com.wowza.wms.server.ServerNotifyBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * @author Alexey Donov
 */
public class ServerListener extends ServerNotifyBase {
    private static final String MAX_FILE_AGE_PROPERTY_NAME = "maxFileAge";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(ServerListener.class);

    private long maxFileAge = 0;

    @NotNull
    private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

    @Nullable
    private ScheduledFuture fileCheckFuture = null;

    private boolean tooOld(File file) {
        return (System.currentTimeMillis() - file.lastModified()) / (1000 * 60 * 60 * 24) > maxFileAge;
    }

    private void logDeletion(File file) {
        logger.info(String.format("File %s is too old, deleting", file.getName()));
    }

    private void checkFiles() {
        final File root = new File("/usr/local/WowzaStreamingEngine");

        //noinspection ResultOfMethodCallIgnored
        Optional.ofNullable(root.listFiles(File::isFile))
            .ifPresent(files -> Stream.of(files)
                .filter(this::tooOld)
                .peek(this::logDeletion)
                .forEach(File::delete));
    }

    private void cancelFuture(ScheduledFuture future) {
        future.cancel(false);
    }

    @Override
    public void onServerInit(IServer server) {
        final WMSProperties properties = server.getProperties();
        maxFileAge = properties.getPropertyLong(MAX_FILE_AGE_PROPERTY_NAME, 0);

        if (maxFileAge > 0) {
            logger.info(String.format("Max file age is %d day(s), starting file age checker", maxFileAge));
            fileCheckFuture = pool.scheduleWithFixedDelay(this::checkFiles, 1, 1, TimeUnit.MINUTES);
        }
    }

    @Override
    public void onServerShutdownStart(IServer server) {
        Optional.ofNullable(fileCheckFuture).ifPresent(this::cancelFuture);
    }
}
