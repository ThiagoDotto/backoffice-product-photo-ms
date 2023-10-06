package br.com.repassa.service.client;

import java.util.Optional;

import br.com.repassa.service.entity.PhotosManager;
import br.com.repassa.service.repository.PhotosManagerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class PhotosManagerRepositoryImpl implements PhotosManagerRepository {

    private final DynamoDB dynamoDB;
    private final String TABLE_NAME = "PhotosManager";

    public PhotosManagerRepositoryImpl(DynamoDB dynamoDBClient) {
        this.dynamoDB = dynamoDBClient;
    }

    @Override
    public Iterable<PhotosManager> findAll(Sort sort) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<PhotosManager> findAll(Pageable pageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends PhotosManager> S save(S entity) {
        Table table = dynamoDB.getTable(TABLE_NAME);

        ObjectMapper map = new ObjectMapper();
        String writeValueAsString = null;
        try {
            writeValueAsString = map.writeValueAsString(entity.getGroupPhotos());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        Item item = new Item()
                .withPrimaryKey("id", entity.getId())
                .withString("editor", entity.getEditor())
                .withString("upload_date", entity.getDate())
                .withString("statusManagerPhotos", entity.getStatusManagerPhotos().toString())
                .withString("groupPhotos", writeValueAsString);

        table.putItem(item);

        return entity;
    }

    @Override
    public <S extends PhotosManager> Iterable<S> saveAll(Iterable<S> entities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<PhotosManager> findById(String id) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public boolean existsById(String id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<PhotosManager> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<PhotosManager> findAllById(Iterable<String> ids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteById(String id) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(PhotosManager entity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll(Iterable<? extends PhotosManager> entities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

}
