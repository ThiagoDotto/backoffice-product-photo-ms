package br.com.repassa.exception;

import br.com.backoffice_repassa_utils_lib.error.interfaces.RepassaUtilError;

public class AwsPhotoError implements RepassaUtilError {

    private static final String APP_PREFIX = "photo";
    private final String errorCode;
    private final String errorMessage;


    public static final RepassaUtilError REKOGNITION_ERROR = new PhotoError("001",
            "Não foi possível ler a imagem.");
    public static final RepassaUtilError REKOGNITION_PHOTO_EMPTY = new PhotoError("002",
            "Nenhuma imagem de etiqueta selecionada para reconhecimento. Verifique as imagens e tente novamente.");
    public static final RepassaUtilError DYNAMO_CONNECTION = new PhotoError("003",
            "Problemas ao conectar ao Dynamo");

    public AwsPhotoError(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    public String getErrorCode() {
        return APP_PREFIX.concat("_").concat(errorCode);
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
