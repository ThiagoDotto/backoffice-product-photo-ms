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
                        "Erro nao esperado ao buscar as Fotos no DynamoDB");
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
        public static final RepassaUtilError GROUP_ERROR = new PhotoError("015",
                "Não foi possível salvar a solicitacao, pois contem um grupo de fotos com erro.");
        public static final RepassaUtilError FOTOS_PRODUTO_NAO_ENCONTRADAS = new PhotoError("016",
                "Não foi possível encontrar as fotos do produto.");
        public static final RepassaUtilError ERROR_VALID_PHOTO = new PhotoError("017",
                "Erro! Algumas imagens não puderam ser carregadas devido ao tipo ou tamanho inválido de arquivo");
        public static final RepassaUtilError BASE64_INVALIDO = new PhotoError("018", "Base64 inválido.");
        public static final RepassaUtilError DELETE_PHOTO = new PhotoError("019", "Problemas ao Deletar a imagem.");
        public static final RepassaUtilError ENDPOINT_NAO_VALIDO = new PhotoError("020", "Endpoint não válido.");

        public static final RepassaUtilError PRODUCT_ID_NULL = new PhotoError("021",
                "Existem PRODUCT_ID's não identificados.");
        public static final RepassaUtilError ERROR_SEARCH_DYNAMODB = new PhotoError("022",
                "Erro ao buscar as informações no Banco de Dados. Tente novamente mais tarde.");

        public static final RepassaUtilError ERROR_VALIDATE_MIMETYPE = new PhotoError("022",
                "Erro ao validar a Extensão do Arquivo.");

        public static final RepassaUtilError ERROR_FAILED_CONNECT_DYNAMODB = new PhotoError("023",
                "Falhar ao conectar com Banco de Dados. Tente novamente mais tarde.");

        public static final RepassaUtilError PHOTOMANAGER_FINISHED = new PhotoError("024",
                "As imagens associadas a esta data foram todas concluídas com sucesso!");

        public static final RepassaUtilError PHOTO_STATUS_ERROR = new PhotoError("025",
                "Essa bag não atende aos requisitos para esta ação!");

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
