package com.tft.coach.vision.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Loads and writes VisionProfile JSON. Unknown resolutions never silently stretch.
 */
public final class VisionProfileLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final String defaultClasspathResource;

    public VisionProfileLoader() {
        this("/vision/profiles/1920x1080-default.json");
    }

    public VisionProfileLoader(String defaultClasspathResource) {
        this.defaultClasspathResource = Objects.requireNonNull(defaultClasspathResource);
    }

    public VisionProfile loadDefault() {
        return loadClasspath(defaultClasspathResource);
    }

    public VisionProfile loadClasspath(String resource) {
        try (InputStream in = VisionProfileLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new UnsupportedProfileException("UNSUPPORTED_PROFILE",
                        "Profile resource missing: " + resource);
            }
            VisionProfile profile = MAPPER.readValue(in, VisionProfile.class);
            RoiValidator.validate(profile);
            return profile;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load profile: " + resource, e);
        }
    }

    /**
     * Resolve by exact resolution against the default baseline profile.
     * Batch A: only 1920x1080 baseline; otherwise UNSUPPORTED_PROFILE.
     */
    public VisionProfile resolveForResolution(int width, int height) {
        VisionProfile baseline = loadDefault();
        if (baseline.resolution().width() == width && baseline.resolution().height() == height) {
            return baseline;
        }
        throw new UnsupportedProfileException(
                "UNSUPPORTED_PROFILE",
                "No calibrated profile for resolution " + width + "x" + height
                        + "; run calibration / choose a known profile"
        );
    }

    public void write(VisionProfile profile, Path target) throws IOException {
        RoiValidator.validate(profile);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        MAPPER.writeValue(target.toFile(), profile);
    }
}
