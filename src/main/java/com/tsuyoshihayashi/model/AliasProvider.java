package com.tsuyoshihayashi.model;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStreamNameAliasProvider;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Object that provides RTSP camera URLs for stream names.
 *
 * @author Alexey Donov
 */
public final class AliasProvider implements IMediaStreamNameAliasProvider {
    /**
     * Singleton instance
     */
    private static @Nullable AliasProvider instance = null;

    public static @NotNull AliasProvider instance() {
        if (instance == null) {
            instance = new AliasProvider();
        }

        return instance;
    }

    private final @NotNull WMSLogger logger = WMSLoggerFactory.getLogger(AliasProvider.class);

    /**
     * Map dictionary that stores associations of stream names with camera information objects
     */
    private final @NotNull Map<String, CameraInfo> cameraInfos = new HashMap<>();

    private AliasProvider() {
        // No op
    }

    /**
     * Get camera information object for the stream name
     *
     * @param streamName Stream name
     * @return RTSP URL
     */
    public @Nullable CameraInfo getCameraInfo(@NotNull String streamName) {
        return cameraInfos.get(streamName);
    }

    /**
     * Associate camera information object with a stream name
     *
     * @param streamName Stream name
     * @param info Camera information object
     */
    public void setCameraInfo(@NotNull String streamName, @Nullable CameraInfo info) {
        logger.info(String.format("Adding URL for camera: %s -> %s", streamName, info));

        cameraInfos.put(streamName, info);
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
        val info = getCameraInfo(name);

        logger.info(String.format("Resolving camera information: %s -> %s", name, info));

        return Optional.ofNullable(info)
            .map(CameraInfo::getUrl)
            .orElse(null);
    }
}
