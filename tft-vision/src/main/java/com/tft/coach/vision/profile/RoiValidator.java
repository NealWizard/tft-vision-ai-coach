package com.tft.coach.vision.profile;

/**
 * Validates ROI bounds against profile resolution / coordinate system.
 */
public final class RoiValidator {

    private RoiValidator() {
    }

    public static void validate(VisionProfile profile) {
        for (RoiRegion region : profile.regions().values()) {
            validateRegion(profile, region);
        }
    }

    public static void validateRegion(VisionProfile profile, RoiRegion region) {
        if (region.width() <= 0 || region.height() <= 0) {
            throw new UnsupportedProfileException("INVALID_ROI",
                    "ROI " + region.id() + " has non-positive size");
        }
        if (region.x() < 0 || region.y() < 0) {
            throw new UnsupportedProfileException("INVALID_ROI",
                    "ROI " + region.id() + " has negative origin");
        }
        if (region.coordinateSystem() == CoordinateSystem.NORMALIZED) {
            if (region.x() + region.width() > 1.0001 || region.y() + region.height() > 1.0001) {
                throw new UnsupportedProfileException("INVALID_ROI",
                        "ROI " + region.id() + " exceeds normalized bounds");
            }
            return;
        }
        int sw = profile.resolution().width();
        int sh = profile.resolution().height();
        if (region.x() + region.width() > sw + 0.5 || region.y() + region.height() > sh + 0.5) {
            throw new UnsupportedProfileException("INVALID_ROI",
                    "ROI " + region.id() + " exceeds screen " + sw + "x" + sh);
        }
    }
}
