package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.dto.ImageDTO;
import br.com.repassa.exception.PhotoError;

import java.text.Normalizer;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

public class PhotosValidate {

    private static final int MAX_SIZE_PHOTO = 15728640;


    public void validatePhotos(ImageDTO imageDTO) throws RepassaException {

        var isValid = new AtomicBoolean(Boolean.TRUE);

        imageDTO.getPhotoBase64().forEach(photo -> {

            byte[] decodedBytes = Base64.getMimeDecoder().decode(photo.getBase64());
            var size = decodedBytes.length;
            var photoIsValid = extensionTypeValidation(photo.getBase64());

            if (!photoIsValid) {
                isValid.set(Boolean.FALSE);
                photo.setNote("invalid file type");
            }

            if(size > MAX_SIZE_PHOTO){
                isValid.set(Boolean.FALSE);
                photo.setNote("file size exceeded");
                photo.setSize(String.valueOf(size));
            }
        });

        if (!isValid.get()){
            throw new RepassaException(PhotoError.ERROR_VALID_PHOTO);
        }
    }

    public String validatePathBucket(String name, String date){

        String username = Normalizer.normalize(name, Normalizer.Form.NFD);
        username = username.toLowerCase();

        String objectKeyWithCount = "fotografia/"
                .concat(username + "/")
                .concat(date + "/");
        return objectKeyWithCount;
    }

    public static Boolean extensionTypeValidation(String photo){

        if (photo.contains("jpeg") || photo.contains("jpg")) {
            return true;
        }
        return false;
    }
}
