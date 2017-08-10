package com.tsuyoshihayashi.wowza;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.module.IModuleOnApp;
import com.wowza.wms.module.IModuleOnStream;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.stream.IMediaStream;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexey Donov
 */
public class Module extends ModuleBase implements IModuleOnApp, IModuleOnStream {
    private @Nullable StreamListener streamListener = null;

    // IModuleOnApp
    @Override
    public void onAppStart(IApplicationInstance instance) {
        streamListener = new StreamListener(instance);
    }

    @Override
    public void onAppStop(IApplicationInstance instance) {
        streamListener = null;
    }

    // IModuleOnStream

    @Override
    public void onStreamCreate(IMediaStream stream) {
        if (streamListener != null) {
            stream.addClientListener(streamListener);
        }
    }

    @Override
    public void onStreamDestroy(IMediaStream stream) {
        if (streamListener != null) {
            stream.removeClientListener(streamListener);
        }
    }
}
