package com.tft.coach.vision.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionProfileTest {

    @Test
    void loadsDefault1080pProfile() {
        VisionProfile profile = new VisionProfileLoader().loadDefault();
        assertEquals("1920x1080-default", profile.profileId());
        assertEquals(1920, profile.resolution().width());
        assertEquals(4, profile.regions().size());
        assertTrue(profile.regions().containsKey("player.gold"));
    }

    @Test
    void unknownResolutionFailsExplicitly() {
        UnsupportedProfileException ex = assertThrows(
                UnsupportedProfileException.class,
                () -> new VisionProfileLoader().resolveForResolution(2560, 1440)
        );
        assertEquals("UNSUPPORTED_PROFILE", ex.errorCode());
    }

    @Test
    void rejectsOutOfBoundsRoi() {
        VisionProfile bad = new VisionProfile(
                "bad",
                "v1",
                new VisionProfile.Resolution(1920, 1080),
                null,
                null,
                null,
                null,
                Map.of("x", new RoiRegion("x", RoiType.TEXT, CoordinateSystem.SCREEN,
                        1900, 0, 100, 10, "x"))
        );
        UnsupportedProfileException ex = assertThrows(
                UnsupportedProfileException.class,
                () -> RoiValidator.validate(bad)
        );
        assertEquals("INVALID_ROI", ex.errorCode());
    }

    @Test
    void writeRoundTrip(@TempDir Path dir) throws Exception {
        VisionProfileLoader loader = new VisionProfileLoader();
        VisionProfile profile = loader.loadDefault();
        Path out = dir.resolve("profile.json");
        loader.write(profile, out);
        VisionProfile reloaded = loader.loadClasspath("/vision/profiles/1920x1080-default.json");
        assertEquals(profile.profileId(), reloaded.profileId());
        assertTrue(out.toFile().exists());
    }
}
