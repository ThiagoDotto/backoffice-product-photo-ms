package br.com.repassa.exception;

import br.com.backoffice_repassa_utils_lib.error.interfaces.RepassaUtilError;

public class PhotoError implements RepassaUtilError {
        private static final String APP_PREFIX = "photo";
        private final String errorCode;
        private final String errorMessage;

        public static final RepassaUtilError ERRO_AO_PERSISTIR = new PhotoError("001",
                        "Erro ao inserir entidade no banco");
        public static final RepassaUtilError QUANTIDADE_FOTO = new PhotoError("002",
                        "A quantidade de fotos, é diferente de 4.");
        public static final RepassaUtilError BUSCAR_DYNAMODB = new PhotoError("003",
                        "Erro ao consultar as informações no banco DynamoDB.");
        public static final RepassaUtilError FOTOS_NAO_ENCONTRADA = new PhotoError("004",
                        "Não há itens encontrados para a data informada. Selecione uma nova data ou tente novamente");
        public static final RepassaUtilError ERRO_AO_BUSCAR_IMAGENS = new PhotoError("005",
                        "Erro nao esperado ao buscar as Fotoso no DynamoDB");
        public static final RepassaUtilError VALIDATE_IDENTIFICATORS_EMPTY = new PhotoError("006",
                        "A lista para validar os ID's se encontra vazia.");
        public static final RepassaUtilError ERRO_AO_SALVAR_NO_DYNAMO = new PhotoError("007",
                        "Erro ao salvar as informações. Tente novamente mais tarde.");
        public static final RepassaUtilError OBJETO_VAZIO = new PhotoError("008", "Objeto vazio!");
        public static final RepassaUtilError SUCESSO_AO_SALVAR = new PhotoError("009",
                        "As edições foram salvas e as imagens foram associadas aos produtos.");
        public static final RepassaUtilError PRODUCT_ID_INVALIDO = new PhotoError("010",
                        "Erro ao verificar se o PRODUCT_ID é válido.");
        public static final RepassaUtilError ALTERAR_STATUS_INVALIDO = new PhotoError("011",
                        "Erro ao tentar atualizar o Status da Imagem.");
        public static final RepassaUtilError PHOTO_MANAGER_IS_NULL = new PhotoError("012",
                        "Não foi possível encontrar nenhum registro com os dados informado.");
        public static final RepassaUtilError REKOGNITION_ERROR = new PhotoError("013",
                        "Não foi possível ler a imagem.");
        public static final RepassaUtilError REKOGNITION_PHOTO_EMPTY = new PhotoError("014",
                        "Nenhuma imagem de etiqueta selecionada para reconhecimento. Verifique as imagens e tente novamente.");
        public static final RepassaUtilError GROUP_ERROR = new PhotoError("015",
                "Não foi possível salvar a solicitacao, pois contem um grupo de fotos com erro.");
        public PhotoError(String errorCode, String errorMessage) {
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
