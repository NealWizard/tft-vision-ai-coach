package com.tft.coach.vision.ocr;

import com.tft.coach.vision.frame.VisionFrame;
import com.tft.coach.vision.frame.VisionFrames;
import com.tft.coach.vision.profile.RoiRegion;
import com.tft.coach.vision.profile.UnsupportedProfileException;
import com.tft.coach.vision.profile.VisionProfileLoader;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoiCropperTest {

    @Test
    void cropsDefaultGoldRoiFrom1080pCanvas() throws Exception {
        VisionFrame frame = VisionFrames.fromImageBytes(solidPng(1920, 1080));
        RoiRegion gold = new VisionProfileLoader().loadDefault().regions().get("player.gold");
        byte[] cropped = RoiCropper.cropPng(frame, gold);
        assertTrue(cropped.length > 0);
        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(cropped));
        assertEquals((int) Math.round(gold.width()), image.getWidth());
        assertEquals((int) Math.round(gold.height()), image.getHeight());
    }

    @Test
    void rejectsRoiOnTinyFixture() {
        VisionFrame tiny = VisionFrames.fromImageBytes(readFixture());
        RoiRegion gold = new VisionProfileLoader().loadDefault().regions().get("player.gold");
        UnsupportedProfileException ex = assertThrows(
                UnsupportedProfileException.class,
                () -> RoiCropper.cropPng(tiny, gold)
        );
        assertEquals("INVALID_ROI", ex.errorCode());
    }

    private static byte[] solidPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] readFixture() {
        try (var in = RoiCropperTest.class.getResourceAsStream("/vision/fixtures/1x1.png")) {
            assert in != null;
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
