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
             photo.setSize(String.valueOf(decodedBytes.length));
            photo.setType(photo.getType().replaceAll("image/", ""));
            var photoIsValid = extensionTypeValidation(photo.getType());

            if (!photoIsValid) {
                isValid.set(Boolean.FALSE);
                photo.setNote("invalid file type");
            }

            if(Integer.valueOf(photo.getSize()) > MAX_SIZE_PHOTO){
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

        String objectKeyWithCount = "fotografia/"
                .concat(username + "/")
                .concat(date + "/");
        return objectKeyWithCount;
    }

    public static Boolean extensionTypeValidation(String type){

        if (type.contains("jpeg") || type.contains("jpg")) {
            return true;
        }
        return false;
    }
}
