package br.com.repassa.service.service;

import br.com.repassa.service.entity.GroupPhotos;
import br.com.repassa.service.entity.Photo;
import br.com.repassa.service.enums.TypeError;

import java.util.ArrayList;
import java.util.List;

public class ManagerGroupPhotos {

    List<GroupPhotos> groupPhotos;

    int totalPhotos = 0;

    public ManagerGroupPhotos(List<GroupPhotos> groupPhotos) {
        this.groupPhotos = groupPhotos;
    }
    public void addPhotos(List<Photo> photos, Boolean isValid) {

        GroupPhotos groupPhotos1 = new GroupPhotos();
        groupPhotos1.setPhotos(new ArrayList<>(photos));

        if(!isValid) {
            groupPhotos1.setTypeError(TypeError.IMAGE_ERROR);
        }

        groupPhotos.add(groupPhotos1);
        totalPhotos += groupPhotos1.getPhotos().size();
    }

    public int getTotalPhotos(){
        return totalPhotos;
    }

}
