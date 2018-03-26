package com.tsuyoshihayashi.wowza;

import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.server.IServer;
import com.wowza.wms.server.ServerNotifyBase;
import lombok.val;
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
 * Object that optionally can check and remove old files from the content directory.
 * Currently is unused - this is being done by cron script.
 *
 * @author Alexey Donov
 */
public final class ServerListener extends ServerNotifyBase {
    private static final String MAX_FILE_AGE_PROPERTY_NAME = "maxFileAge";

    private final WMSLogger logger = WMSLoggerFactory.getLogger(ServerListener.class);

    private long maxFileAge = 0;

    @NotNull
    private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

    /**
     * Variable that stores the checking schedule
     */
    @Nullable
    private ScheduledFuture fileCheckFuture = null;

    /**
     * Determine if the file is considered old
     *
     * @param file File object
     * @return true if the file is old
     */
    private boolean tooOld(File file) {
        return (System.currentTimeMillis() - file.lastModified()) / (1000 * 60 * 60 * 24) > maxFileAge;
    }

    /**
     * Log the delete action
     *
     * @param file File being deleted
     */
    private void logDeletion(File file) {
        logger.info(String.format("File %s is too old, deleting", file.getName()));
    }

    /**
     * Check the list of files in the content directory and delete ones that are old
     */
    private void checkFiles() {
        val root = new File("/usr/local/WowzaStreamingEngine");

        //noinspection ResultOfMethodCallIgnored
        Optional.ofNullable(root.listFiles(File::isFile))
            .ifPresent(files -> Stream.of(files)
                .filter(this::tooOld)
                .peek(this::logDeletion)
                .forEach(File::delete));
    }

    /**
     * Stop checking the content directory
     *
     * @param future Schedule object
     */
    private void cancelFuture(ScheduledFuture future) {
        future.cancel(false);
    }

    /**
     * When the server starts, schedule the checks every 1 minute
     *
     * @param server Server object
     */
    @Override
    public void onServerInit(IServer server) {
        val properties = server.getProperties();
        maxFileAge = properties.getPropertyLong(MAX_FILE_AGE_PROPERTY_NAME, 0);

        if (maxFileAge > 0) {
            logger.info(String.format("Max file age is %d day(s), starting file age checker", maxFileAge));
            fileCheckFuture = pool.scheduleWithFixedDelay(this::checkFiles, 1, 1, TimeUnit.MINUTES);
        }
    }

    /**
     * When the server stops, remove the checking schedule
     *
     * @param server Server object
     */
    @Override
    public void onServerShutdownStart(IServer server) {
        Optional.ofNullable(fileCheckFuture).ifPresent(this::cancelFuture);
    }
}
