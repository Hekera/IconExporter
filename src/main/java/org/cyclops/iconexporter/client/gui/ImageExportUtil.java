package org.cyclops.iconexporter.client.gui;

import com.google.common.base.Charsets;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.nbt.INBT;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.cyclops.iconexporter.IconExporter;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;

/**
 * Utilities for exporting images to files.
 * @author rubensworks
 */
public class ImageExportUtil {

    public static void exportImageFromScreenshot(File dir, String key, int guiWidth, int guiHeight, int scale, int backgroundColor) throws IOException {
        // Take a screenshot
        NativeImage image = createScreenshot(guiWidth, guiHeight, Minecraft.getInstance().getFramebuffer());
        float imageScale = image.getWidth() / guiWidth;
        image = getSubImage(image, (int) (scale * imageScale), (int) (scale * imageScale));

        // Convert our background color to a fully transparent pixel
        byte alpha = (byte) 256;
        alpha %= 0xff;
        for (int cx = 0; cx < image.getWidth(); cx++) {
            for (int cy = 0; cy < image.getHeight(); cy++) {
                int color = image.getPixelRGBA(cx, cy);

                if (color == backgroundColor) {
                    color = 0;
                    int mc = (alpha << 24) | 0x00ffffff;
                    int newcolor = color & mc;
                    image.setPixelRGBA(cx, cy, newcolor);
                }
            }
        }

        // Write the file
        key = key
                .replaceAll(":", "__")
                .replaceAll("\"", "'");
        try {
            File file = new File(dir, key + ".png").getCanonicalFile();
            try {
                image.write(file);
            } catch (NullPointerException e) {
                e.printStackTrace();
                throw new IOException("Error while writing the PNG image " + file);
            }
        } catch (IOException e) {
            IconExporter.clog(Level.ERROR, "Error while writing the PNG image for key " + key);
            throw e;
        }
    }

    public static void exportNbtFile(File dir, String key, INBT tag) throws IOException {
        // Write the file
        key = key.replaceAll(":", "__");
        try {
            File file = new File(dir, key + ".txt").getCanonicalFile();
            try {
                FileUtils.writeStringToFile(file, tag.toString(), Charsets.UTF_8);
            } catch (NullPointerException e) {
                e.printStackTrace();
                throw new IOException("Error while writing the TXT image " + file);
            }
        } catch (IOException e) {
            IconExporter.clog(Level.ERROR, "Error while writing the TXT image for key " + key);
            throw e;
        }
    }

    public static NativeImage getSubImage(NativeImage image, int width, int height) {
        NativeImage imageNew = new NativeImage(width, height, false);

        // Modified from NativeImage#copyImageData
        for(int y = 0; y < imageNew.getHeight(); y++) {
            int pointerOffset = y * image.getWidth() * image.getFormat().getPixelSize();
            int pointerOffsetNew = y * imageNew.getWidth() * imageNew.getFormat().getPixelSize();
            MemoryUtil.memCopy(image.imagePointer + (long)pointerOffset, imageNew.imagePointer + (long)pointerOffsetNew, (long)imageNew.getWidth() * image.getFormat().getPixelSize()); // changed here to multiply number of bytes with pixel size
        }

        return imageNew;
    }

    // Adapted from net.minecraft.util.ScreenShotHelper to create buffered images with alpha
    public static NativeImage createScreenshot(int width, int height, Framebuffer framebufferIn) {
        if (GLX.isUsingFBOs()) {
            width = framebufferIn.framebufferTextureWidth;
            height = framebufferIn.framebufferTextureHeight;
        }

        NativeImage nativeimage = new NativeImage(width, height, false);
        if (GLX.isUsingFBOs()) {
            GlStateManager.bindTexture(framebufferIn.framebufferTexture);
            nativeimage.downloadFromTexture(0, true);
        } else {
            nativeimage.downloadFromFramebuffer(true);
        }

        nativeimage.flip();
        return nativeimage;
    }

}
