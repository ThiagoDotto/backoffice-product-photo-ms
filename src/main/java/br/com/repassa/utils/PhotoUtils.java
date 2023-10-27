package br.com.repassa.utils;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhotoUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoUtils.class);

    public static String thumbnail (String urlStr) {
        try {
            URL url = new URL(urlStr);
            BufferedImage originalImage = ImageIO.read(url);
            int newWidth = 180;
            int newHeight = 180;
            Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

            BufferedImage resizedBufferedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            resizedBufferedImage.getGraphics().drawImage(resizedImage, 0, 0, new ImageObserver() {
                @Override
                public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                    return false;
                }
            });

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedBufferedImage, "jpg", byteArrayOutputStream);
            byte[] resizedImageBytes = byteArrayOutputStream.toByteArray();

            return Base64.getEncoder().encodeToString(resizedImageBytes);
        } catch (MalformedURLException e) {
            LOG.error("Error 1" + e.getMessage());
        } catch (IOException e) {
            LOG.error("Error 2" + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error 3" + e.getMessage());
        }
        return "";
    }
}
