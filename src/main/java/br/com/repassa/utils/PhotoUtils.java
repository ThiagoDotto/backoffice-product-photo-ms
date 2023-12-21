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
import java.util.List;
import javax.imageio.ImageIO;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.entity.Photo;
import br.com.repassa.exception.PhotoError;
import com.amazonaws.services.lambda.runtime.Context;
import org.apache.tika.Tika;
import org.apache.tika.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhotoUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoUtils.class);

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

    public static void urlToBase64AndMimeType(String urlStr, Photo photo) throws RepassaException {
        try{
            String base64 = urlToBase64(urlStr);
            String mimeType = getMimeTypeFromBase64(base64);

            photo.setMimeType(mimeType);
        } catch (IOException e) {
            throw new RepassaException(PhotoError.ERROR_VALIDATE_MIMETYPE);
        }
    }

    public static String extractDataBase64(String base64) {
        String[] parts = base64.split(",");

        return parts[1];
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
