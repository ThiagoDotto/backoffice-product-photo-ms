package br.com.repassa.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import br.com.repassa.dto.PhotoFilterResponseDTO;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.exception.PhotoError;
import br.com.repassa.enums.StatusManagerPhotos;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class PhotoClient implements PhotoClientInterface {
    private static final String TABLE_NAME = "PhotoProcessingTable_QA";
    private static final String TABLE_NAME_PHOTOS = "PhotosManager_QA";

    private DynamoDbClient dynamoDB;

    private PhotosManagerRepositoryImpl impl;

    Table table;
    Table tablePhotosManager;

    public PhotoClient() {
        this.dynamoDB = new DynamoDbClient();
        impl = new PhotosManagerRepositoryImpl(this.dynamoDB.openDynamoDBConnection());
        this.table = this.dynamoDB.openDynamoDBConnection().getTable(TABLE_NAME);
        this.tablePhotosManager = this.dynamoDB.openDynamoDBConnection().getTable(TABLE_NAME_PHOTOS);
    }

    public void savePhotosManager(PhotosManager manager) {
        impl.save(manager);
    }

    public List<PhotoFilterResponseDTO> listItem(String fieldFiltered, Map<String, Object> expressionAttributeValues)
            throws RepassaException {

        try {
            ItemCollection<ScanOutcome> items = table.scan(
                    "contains(upload_date, :upload_date) AND edited_by = :edited_by", fieldFiltered, null,
                    expressionAttributeValues);

            List<PhotoFilterResponseDTO> photoFilterResponseDTOS = mapPhotoFilter(items);

            if (photoFilterResponseDTOS.size() == 0){
                log.error("Não há itens encontrados para a data informada. Selecione uma nova data ou tente novamente");
                throw new RepassaException(PhotoError.FOTOS_NAO_ENCONTRADA);
            }

            return photoFilterResponseDTOS;
        } catch (AmazonDynamoDBException e) {
            log.error("Erro ao consultar as informações no banco DynamoDB.");
            throw new RepassaException(PhotoError.BUSCAR_DYNAMODB);
        } catch (RepassaException e) {
            log.error("Nenhuma foto encontrada para essa data.");
            throw new RepassaException(PhotoError.FOTOS_NAO_ENCONTRADA);
        } catch (Exception e) {
            log.error("Erro nao esperado ao buscar as Fotoso no DynamoDB");
            throw new RepassaException(PhotoError.ERRO_AO_BUSCAR_IMAGENS);
        }
    }

    public PhotosManager getPhotos(String fieldFiltered, Map<String, Object> expressionAttributeValues)
            throws RepassaException {
        PhotosManager responseDTO = null;
        try {

            ItemCollection<ScanOutcome> items = tablePhotosManager.scan(
                    "statusManagerPhotos = :statusManagerPhotos and contains(upload_date, :upload_date) and editor = :editor", fieldFiltered, null,
                    expressionAttributeValues);

            for (Item item : items) {
                responseDTO = new PhotosManager();
                responseDTO.setId(item.getString("id"));
                responseDTO.setDate(item.getString("upload_date"));
                responseDTO.setEditor(item.getString("editor"));
                responseDTO.setStatusManagerPhotos(StatusManagerPhotos.valueOf(item.getString("statusManagerPhotos")));
                String json = item.getString("groupPhotos");
                ObjectMapper objectMapper = new ObjectMapper();
                List<GroupPhotos> readValue = objectMapper.readValue(json, new TypeReference<List<GroupPhotos>>(){});
                responseDTO.setGroupPhotos(readValue);

            }

            return responseDTO;
        } catch (AmazonDynamoDBException e) {
            log.error("Erro ao consultar as informações no banco DynamoDB.");
            e.printStackTrace();
            throw new RepassaException(PhotoError.BUSCAR_DYNAMODB);
        }  catch (Exception e) {
            log.error("Erro nao esperado ao buscar as Fotoso no DynamoDB");
            e.printStackTrace();
            throw new RepassaException(PhotoError.ERRO_AO_BUSCAR_IMAGENS);
        }
    }

    private List<PhotoFilterResponseDTO> mapPhotoFilter(ItemCollection<ScanOutcome> items) {

        List<PhotoFilterResponseDTO> resultList = new ArrayList<>();

        for (Item item : items) {
            String imageName = item.getString("imagem_name");

            if (imageName == null) {
                imageName = "undefined";
            }

            PhotoFilterResponseDTO responseDTO = new PhotoFilterResponseDTO();
            responseDTO.setBagId(item.getString("bag_id"));
            responseDTO.setEditedBy(item.getString("edited_by"));
            responseDTO.setImageName(imageName);
            responseDTO.setId(item.getString("id"));
            responseDTO.setImageId(item.getString("image_id"));
            responseDTO.setIsValid(item.getString("is_valid"));
            responseDTO.setOriginalImageUrl(item.getString("original_image_url"));
            responseDTO.setSizePhoto(item.getString("size_photo"));
            responseDTO.setThumbnailBase64(item.getString("thumbnail_base64"));
            responseDTO.setUploadDate(item.getString("upload_date"));

            resultList.add(responseDTO);
        }

        Collections.sort(resultList,
                Comparator.nullsFirst(Comparator.comparing(PhotoFilterResponseDTO::getImageName)));

        return resultList;
    }
}
