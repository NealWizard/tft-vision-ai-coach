package com.tft.coach.vision.frame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameSourceTest {

    @Test
    void fixtureFrameSourceReadsOnce() {
        try (FixtureFrameSource source = FixtureFrameSource.defaultFixture()) {
            Optional<VisionFrame> first = source.nextFrame();
            assertTrue(first.isPresent());
            assertEquals(1, first.get().width());
            assertEquals(1, first.get().height());
            assertTrue(source.nextFrame().isEmpty());
        }
    }

    @Test
    void fileAndDirectorySources(@TempDir Path dir) throws Exception {
        byte[] png = readFixtureBytes();
        Path a = dir.resolve("a.png");
        Path b = dir.resolve("b.png");
        Files.write(a, png);
        Files.write(b, png);

        try (FileFrameSource file = new FileFrameSource(a)) {
            assertTrue(file.nextFrame().isPresent());
            assertTrue(file.nextFrame().isEmpty());
        }

        try (DirectoryFrameSource directory = new DirectoryFrameSource(dir)) {
            assertTrue(directory.metadata().description().contains("2"));
            assertTrue(directory.nextFrame().isPresent());
            assertTrue(directory.nextFrame().isPresent());
            assertTrue(directory.nextFrame().isEmpty());
        }
    }

    @Test
    void closedSourceRejects(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("x.png");
        Files.write(file, readFixtureBytes());
        FileFrameSource source = new FileFrameSource(file);
        source.close();
        try {
            source.nextFrame();
            assertFalse(true, "expected closed failure");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("closed"));
        }
    }

    private static byte[] readFixtureBytes() throws Exception {
        try (var in = FrameSourceTest.class.getResourceAsStream("/vision/fixtures/1x1.png")) {
            assert in != null;
            return in.readAllBytes();
        }
    }
}
