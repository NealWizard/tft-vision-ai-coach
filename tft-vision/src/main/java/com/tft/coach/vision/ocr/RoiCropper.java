package com.tft.coach.vision.ocr;

import com.tft.coach.vision.frame.FramePayload;
import com.tft.coach.vision.frame.VisionFrame;
import com.tft.coach.vision.profile.CoordinateSystem;
import com.tft.coach.vision.profile.RoiRegion;
import com.tft.coach.vision.profile.UnsupportedProfileException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Crops a SCREEN ROI from a frame using ImageIO.
 */
public final class RoiCropper {

    private RoiCropper() {
    }

    public static byte[] cropPng(VisionFrame frame, RoiRegion roi) {
        BufferedImage image = read(frame.payload());
        int x;
        int y;
        int w;
        int h;
        if (roi.coordinateSystem() == CoordinateSystem.NORMALIZED) {
            x = (int) Math.round(roi.x() * image.getWidth());
            y = (int) Math.round(roi.y() * image.getHeight());
            w = (int) Math.round(roi.width() * image.getWidth());
            h = (int) Math.round(roi.height() * image.getHeight());
        } else {
            x = (int) Math.round(roi.x());
            y = (int) Math.round(roi.y());
            w = (int) Math.round(roi.width());
            h = (int) Math.round(roi.height());
        }
        if (w <= 0 || h <= 0 || x < 0 || y < 0
                || x + w > image.getWidth() || y + h > image.getHeight()) {
            throw new UnsupportedProfileException(
                    "INVALID_ROI",
                    "ROI " + roi.id() + " does not fit " + image.getWidth() + "x" + image.getHeight()
            );
        }
        BufferedImage cropped = image.getSubimage(x, y, w, h);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(cropped, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode cropped PNG", e);
        }
    }

    public static String cropPngBase64(VisionFrame frame, RoiRegion roi) {
        return Base64.getEncoder().encodeToString(cropPng(frame, roi));
    }

    private static BufferedImage read(FramePayload payload) {
        try {
            byte[] bytes = switch (payload) {
                case FramePayload.InlineBytes inline -> inline.bytes();
                case FramePayload.LocalFile local -> Files.readAllBytes(local.path());
                case FramePayload.SharedFile shared -> Files.readAllBytes(shared.path());
            };
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IllegalArgumentException("INVALID_IMAGE");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode frame image", e);
        }
    }
}
