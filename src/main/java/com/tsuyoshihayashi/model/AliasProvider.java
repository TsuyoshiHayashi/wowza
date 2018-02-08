package com.tsuyoshihayashi.model;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStreamNameAliasProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexey Donov
 */
public final class AliasProvider implements IMediaStreamNameAliasProvider {
    private static AliasProvider instance = null;

    public static AliasProvider instance() {
        if (instance == null) {
            instance = new AliasProvider();
        }

        return instance;
    }

    private final WMSLogger logger = WMSLoggerFactory.getLogger(AliasProvider.class);

    private final Map<String, String> cameraURLs = new HashMap<>();

    private AliasProvider() {

    }

    private String getCameraURL(String streamName) {
        return cameraURLs.get(streamName);
    }

    public void setCameraIP(String streamName, String ip) {
        logger.info(String.format("Adding URL for camera: %s -> %s", streamName, ip));

        cameraURLs.put(streamName, ip);
    }

    @Override
    public String resolvePlayAlias(IApplicationInstance instance, String name) {
        return name;
    }

    @Override
    public String resolveStreamAlias(IApplicationInstance instance, String name) {
        final String url = getCameraURL(name);
        logger.info(String.format("Resolving camera URL: %s -> %s", name, url));

        return url;
    }
}
