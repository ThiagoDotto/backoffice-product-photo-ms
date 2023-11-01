package br.com.repassa.service;

import br.com.repassa.dto.ImageDTO;

import java.text.Normalizer;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

public class PhotosValidate {

    private static final int MAX_SIZE_PHOTO = 15728640;


    public boolean validate(ImageDTO imageDTO) {

        var isValid = new AtomicBoolean(Boolean.TRUE);

        imageDTO.getPhotoBase64().forEach(photo -> {

            byte[] decodedBytes = Base64.getMimeDecoder().decode(photo.getBase64());
            photo.setSize(String.valueOf(decodedBytes.length));
            photo.setType(photo.getType().replaceAll("image/", ""));
            var photoIsValid = extensionTypeValidation(photo.getType());

            if (Boolean.FALSE.equals(photoIsValid)) {
                isValid.set(Boolean.FALSE);
                photo.setNote("invalid file type");
            }

            if (Integer.parseInt(photo.getSize()) > MAX_SIZE_PHOTO) {
                isValid.set(Boolean.FALSE);
                photo.setNote("file size exceeded");
            }
        });
        return isValid.get();

    }

    public String validatePathBucket(String name, String date) {

        String username = Normalizer.normalize(name, Normalizer.Form.NFD);
        username = username.toLowerCase();

        String objectKeyWithCount = "fotografia/"
                .concat(username + "/")
                .concat(date + "/");
        return objectKeyWithCount;
    }

    public static Boolean extensionTypeValidation(String type) {

        if (type.contains("jpeg") || type.contains("jpg")) {
            return true;
        }
        return false;
    }
}
