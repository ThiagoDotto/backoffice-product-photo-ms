package br.com.repassa.service;

import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.Photo;
import br.com.repassa.enums.StatusProduct;
import br.com.repassa.enums.TypeError;
import br.com.repassa.enums.TypePhoto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ManagerGroupPhotos {

    List<GroupPhotos> groupPhotos;

    int totalPhotos = 0;

    public ManagerGroupPhotos(List<GroupPhotos> groupPhotos) {
        this.groupPhotos = groupPhotos;
    }
    public void addPhotos(List<Photo> photos, Boolean isValid) {

        GroupPhotos groupPhotos1 = new GroupPhotos();
        groupPhotos1.setStatusProduct(StatusProduct.EM_ANDAMENTO);
        groupPhotos1.setUpdateDate(LocalDateTime.now().toString());
        groupPhotos1.setId(UUID.randomUUID().toString());
        groupPhotos1.setPhotos(new ArrayList<>(photos));

        if(!isValid) {
            groupPhotos1.setImageError(TypeError.IMAGE_ERROR.name());
        }

        groupPhotos.add(groupPhotos1);
        totalPhotos += groupPhotos1.getPhotos().size();
    }

    public int getTotalPhotos(){
        return totalPhotos;
    }

}
