package com.laboschqpa.filehost.util;

import com.laboschqpa.filehost.entity.ImageVariant;

import java.util.List;

public class ImageVariantSelector {
    public static final float MAX_RELATIVE_SIZE_POSITION_TO_SERVE_THE_SMALLER_SIZED_VARIANT = 0.33f;

    public static ImageVariant selectImageVariantToServe(List<ImageVariant> variants, int wantedImageSize) {
        ImageVariant closestSmaller = null;
        ImageVariant closestLarger = null;

        for (ImageVariant variant : variants) {
            final int currentSize = variant.getVariantSize();
            if (currentSize == wantedImageSize) {
                return variant;
            }

            final boolean isVariantSmallerThanWanted = currentSize < wantedImageSize;

            if (isVariantSmallerThanWanted) {
                closestSmaller = max(closestSmaller, variant);
            } else {
                closestLarger = min(closestLarger, variant);
            }
        }

        if (closestSmaller != null && closestLarger != null) {
            return getWinnerOfTwoClosestVariants(closestSmaller, closestLarger, wantedImageSize);
        }
        if (closestSmaller != null) {
            return closestSmaller;
        }
        if (closestLarger != null) {
            return closestLarger;
        }

        throw new IllegalStateException("Could not select ImageVariant to server. The provided variant list is probably empty!");
    }

    private static ImageVariant min(ImageVariant closestRightNow, ImageVariant examined) {
        if (closestRightNow == null) {
            return examined;
        }

        if (closestRightNow.getVariantSize() < examined.getVariantSize()) {
            return closestRightNow;
        }
        return examined;
    }

    private static ImageVariant max(ImageVariant closestRightNow, ImageVariant examined) {
        if (closestRightNow == null) {
            return examined;
        }

        if (closestRightNow.getVariantSize() > examined.getVariantSize()) {
            return closestRightNow;
        }
        return examined;
    }

    /**
     * The intention is to serve the larger variant unless the smaller variant is <i>really</i> close to the wanted size.
     */
    private static ImageVariant getWinnerOfTwoClosestVariants(ImageVariant closestSmaller, ImageVariant closestLarger, int wantedSize) {
        final int smallerSize = closestSmaller.getVariantSize();
        final int largerSize = closestLarger.getVariantSize();

        final int smallerDelta = wantedSize - smallerSize;
        final int largerDelta = largerSize - wantedSize;
        final int fullDiff = smallerDelta + largerDelta;

        final float relativeSizePosition = smallerDelta / (float) fullDiff;

        if (relativeSizePosition < MAX_RELATIVE_SIZE_POSITION_TO_SERVE_THE_SMALLER_SIZED_VARIANT) {
            return closestSmaller;
        }
        return closestLarger;
    }
}
