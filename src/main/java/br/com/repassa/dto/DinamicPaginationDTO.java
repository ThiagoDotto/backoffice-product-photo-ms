package br.com.repassa.dto;

import br.com.repassa.entity.PhotosManager;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DinamicPaginationDTO {

    private PhotosManager objectPagination;

    private String lastObjectId;

    public DinamicPaginationDTO() {
    }

    public DinamicPaginationDTO(PhotosManager photosManager, List<PhotoFilterResponseDTO> photosProcessing) {
        this.objectPagination = photosManager;
        this.lastObjectId = lastObjectId(photosProcessing);
    }

    private String lastObjectId(List<PhotoFilterResponseDTO> objectPagination) {
        return objectPagination.get(objectPagination.size() - 1).getImageId();
    }


}
