package com.tsuyoshihayashi.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexey Donov
 */
@Data
public final class CameraInfo {
    private final @NotNull String url;
    private final @Nullable String title;
    private final @Nullable String comment;
    private final @Nullable TextAction textAction;
}
