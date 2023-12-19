package br.com.repassa.utils;

import br.com.repassa.entity.Photo;
import com.amazonaws.services.lambda.runtime.Context;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.List;

public class CommonsUtil {
    static String INVALID_PHOTO = "invalidPhoto.png";
	public static Boolean mimeTypeValid(String mimeType) {
		List<Object> mimeTypes = List.of("image/jpeg");

		return mimeTypes.contains(mimeType.toLowerCase());
	}

    public static void validatePhoto(Long size, Photo photo, String basePath) {
        String pathError = basePath + INVALID_PHOTO;

        boolean mimeTypeIsNotValid = !mimeTypeValid(photo.getMimeType());
        boolean sizeIsNotValid = size > 15728640;

        photo.setIsValid(true);

        if(sizeIsNotValid && mimeTypeIsNotValid) {
            photoBuild(photo, pathError, "Tamanho e Formato do arquivo inválido. Por favor tente novamente.");
        } else if(sizeIsNotValid) {
            photoBuild(photo, pathError, "Tamanho do arquivo inválido. São aceitos arquivos de até 15Mb.");
        } else if(mimeTypeIsNotValid) {
            photoBuild(photo, pathError, "Formato de arquivo inválido. São aceitos somente JPG ou JPEG.");
        }
    }

    private static void photoBuild(Photo photo, String pathError, String message) {
        photo.setNote(message);
        photo.setNamePhoto(INVALID_PHOTO);
        photo.setUrlPhoto(pathError);
        photo.setUrlThumbnail(pathError);
        photo.setIsValid(false);
    }
}
