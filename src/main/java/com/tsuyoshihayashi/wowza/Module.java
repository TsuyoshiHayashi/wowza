package com.tsuyoshihayashi.wowza;

import com.tsuyoshihayashi.model.AliasProvider;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.module.IModuleOnApp;
import com.wowza.wms.module.IModuleOnStream;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.stream.IMediaStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Wowza module object that sets up:
 * - Stream alias provider
 * - Stream listener
 * - Record manager
 *
 * @author Alexey Donov
 */
public final class Module extends ModuleBase implements IModuleOnApp, IModuleOnStream {
    private static final String UPLOAD_OVERRIDE_ENDPOINT_KEY = "uploadOverrideEndpoint";

    private @Nullable StreamListener streamListener = null;
    private final @NotNull RecordManagerListener recordManagerListener = new RecordManagerListener();

    // IModuleOnApp

    /**
     * Set up record listener, stream listener and alias provider fo the application instance
     *
     * @param instance Live application instance
     */
    @Override
    public void onAppStart(IApplicationInstance instance) {
        RecorderListener.uploadOverrideEndpoint = instance.getProperties().getPropertyStr(UPLOAD_OVERRIDE_ENDPOINT_KEY);

        streamListener = new StreamListener(instance);
        instance.getVHost().getLiveStreamRecordManager().addListener(recordManagerListener);

        instance.setStreamNameAliasProvider(AliasProvider.instance());
    }

    /**
     * When the application is stoppped, remove listeners
     *
     * @param instance Live application instance
     */
    @Override
    public void onAppStop(IApplicationInstance instance) {
        streamListener = null;
        instance.getVHost().getLiveStreamRecordManager().removeListener(recordManagerListener);
    }

    // IModuleOnStream

    /**
     * When the stream is created, add a stream listener to it
     *
     * @param stream Stream
     */
    @Override
    public void onStreamCreate(IMediaStream stream) {
        Optional.ofNullable(streamListener).ifPresent(stream::addClientListener);
    }

    /**
     * When the stream is destroyed, stream listener no longer needs to listen to it
     *
     * @param stream Stream
     */
    @Override
    public void onStreamDestroy(IMediaStream stream) {
        Optional.ofNullable(streamListener).ifPresent(stream::removeClientListener);
    }
}
