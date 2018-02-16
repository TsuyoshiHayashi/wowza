package com.tsuyoshihayashi.model;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStreamNameAliasProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Object that provides RTSP camera URLs for stream names.
 *
 * @author Alexey Donov
 */
public final class AliasProvider implements IMediaStreamNameAliasProvider {
    /**
     * Singleton instance
     */
    private static AliasProvider instance = null;

    public static AliasProvider instance() {
        if (instance == null) {
            instance = new AliasProvider();
        }

        return instance;
    }

    private final WMSLogger logger = WMSLoggerFactory.getLogger(AliasProvider.class);

    /**
     * Map dictionary that stores associations of stream names with RTSP URLs
     */
    private final Map<String, String> cameraURLs = new HashMap<>();

    private AliasProvider() {

    }

    /**
     * Get a RTSP URL for the stream name
     *
     * @param streamName Stream name
     * @return RTSP URL
     */
    private String getCameraURL(String streamName) {
        return cameraURLs.get(streamName);
    }

    /**
     * Associate a RTSP URL with a stream name
     *
     * @param streamName Stream name
     * @param url RTSP URL
     */
    public void setCameraURL(String streamName, String url) {
        logger.info(String.format("Adding URL for camera: %s -> %s", streamName, url));

        cameraURLs.put(streamName, url);
    }

    /**
     * Resolve stream name for the player. In this case player stream name is unchanged.
     *
     * @param instance Application instance
     * @param name Stream name
     * @return Stream name
     */
    @Override
    public String resolvePlayAlias(IApplicationInstance instance, String name) {
        return name;
    }

    /**
     * Resolve RTSP URL for the broadcaster when asked by Media Caster
     *
     * @param instance Application instance
     * @param name Stream name
     * @return RTSP URL
     */
    @Override
    public String resolveStreamAlias(IApplicationInstance instance, String name) {
        final String url = getCameraURL(name);
        logger.info(String.format("Resolving camera URL: %s -> %s", name, url));

        return url;
    }
}
