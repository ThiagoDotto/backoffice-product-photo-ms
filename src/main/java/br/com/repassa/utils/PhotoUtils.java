package br.com.repassa.utils;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import javax.imageio.ImageIO;

import br.com.repassa.entity.Photo;
import org.apache.tika.Tika;
import org.apache.tika.io.IOUtils;
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

    public static String urlToBase64(String urlStr) throws IOException {
        URL imageUrl = new URL(urlStr);
        URLConnection ucon = imageUrl.openConnection();

        try (InputStream is = ucon.getInputStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;

            while ((read = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, read);
            }

            baos.flush();

            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }

    public static void urlToBase64AndMimeType(String urlStr, Photo photo) throws IOException {
        String base64 = urlToBase64(urlStr);
        String mimeType = getMimeTypeFromBase64(base64);

        photo.setMimeType(mimeType);
    }
    public static String getMimeTypeFromBase64(String base64Image) throws IOException {
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);

        try {
            Tika tika = new Tika();
            return tika.detect(bais);
        } finally {
            IOUtils.closeQuietly(bais);
        }
    }
}
