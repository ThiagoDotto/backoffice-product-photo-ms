package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.dto.ImageDTO;
import br.com.repassa.exception.PhotoError;

import java.text.Normalizer;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

public class PhotosValidate {

    public void validatePhotos(ImageDTO imageDTO) throws RepassaException {

        var isValid = new AtomicBoolean(Boolean.TRUE);

        imageDTO.getPhotoBase64().forEach(photo -> {

            byte[] decodedBytes = Base64.getMimeDecoder().decode(photo.getBase64());
            var size = decodedBytes.length;
            var photoIsValid = photoIsValid(photo.getBase64());

            if (!photoIsValid) {
                isValid.set(Boolean.FALSE);
                photo.setNote("invalid file type");
            }

            if(size > 15728640){
                isValid.set(Boolean.FALSE);
                photo.setNote("file size exceeded");
            }
        });

        if (!isValid.get()){
            throw new RepassaException(PhotoError.ERROR_VALID_PHOTO);
        }
    }

    public String validatePathBucket(String name, String date){

        String username = Normalizer.normalize(name, Normalizer.Form.NFD);
        username = username.toLowerCase();
//        username = username.replaceAll("\\s", "+");
//        username = username.replaceAll("[^a-zA-Z0-9+]", "");

        String objectKeyWithCount = "fotografia/"
                .concat(username + "/")
                .concat(date + "/");
        return objectKeyWithCount;
    }

    public static Boolean photoIsValid(String photo){

        if (photo.contains("jpeg") || photo.contains("jpg")) {
            return true;
        }
        return false;
    }
}
