package br.com.repassa.service;

import java.text.Normalizer;
import java.util.Base64;

import br.com.repassa.dto.PhotoBase64DTO;
import br.com.repassa.dto.PhotoInsertValidateDTO;

public class PhotosValidate {

    private static final int MAX_SIZE_PHOTO = 15728640;

    public PhotoInsertValidateDTO validatePhoto(PhotoBase64DTO photo) {
        PhotoInsertValidateDTO responseDto = new PhotoInsertValidateDTO();

        byte[] decodedBytes = org.apache.commons.codec.binary.Base64.decodeBase64(photo.getBase64());
        photo.setSize(String.valueOf(decodedBytes.length));
        photo.setType(photo.getType().replaceAll("image/", ""));
        var photoIsValid = extensionTypeValidation(photo.getType());

        if (Boolean.FALSE.equals(photoIsValid)) {
            responseDto.setValid(false);
            photo.setNote("Formato de arquivo inválido. São aceitos somente JPG ou JPEG");
        }

        if (isGreatThanMaxSize(photo.getSize())) {
            responseDto.setValid(false);
            photo.setNote("Tamanho do arquivo inválido. São aceitos arquivos de até 15Mb");
        }
        responseDto.setPhoto(photo);
        return responseDto;
    }

    public static boolean isGreatThanMaxSize(String photoMaxSize) {
        return Integer.parseInt(photoMaxSize) > MAX_SIZE_PHOTO;
    }

    public String validatePathBucket(String name, String date) {

        String username = Normalizer.normalize(name, Normalizer.Form.NFD);
        username = username.toLowerCase();

        String objectKeyWithCount = "fotografia/"
                .concat(username + "/")
                .concat(date + "/");
        return objectKeyWithCount;
    }

    public static boolean extensionTypeValidation(String type) {

        if (type.contains("jpeg") || type.contains("jpg")) {
            return true;
        }
        return false;
    }
}
