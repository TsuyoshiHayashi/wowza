package com.tsuyoshihayashi.wowza;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.module.IModuleOnApp;
import com.wowza.wms.module.IModuleOnStream;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.stream.IMediaStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @author Alexey Donov
 */
public class Module extends ModuleBase implements IModuleOnApp, IModuleOnStream {
    private @Nullable StreamListener streamListener = null;
    private final @NotNull RecordManagerListener recordManagerListener = new RecordManagerListener();

    // IModuleOnApp
    @Override
    public void onAppStart(IApplicationInstance instance) {
        streamListener = new StreamListener(instance);
        instance.getVHost().getLiveStreamRecordManager().addListener(recordManagerListener);
    }

    @Override
    public void onAppStop(IApplicationInstance instance) {
        streamListener = null;
        instance.getVHost().getLiveStreamRecordManager().removeListener(recordManagerListener);
    }

    // IModuleOnStream

    @Override
    public void onStreamCreate(IMediaStream stream) {
        Optional.ofNullable(streamListener).ifPresent(stream::addClientListener);
    }

    @Override
    public void onStreamDestroy(IMediaStream stream) {
        Optional.ofNullable(streamListener).ifPresent(stream::removeClientListener);
    }
}
