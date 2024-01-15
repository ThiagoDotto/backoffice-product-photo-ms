package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.dto.PhotoFilterDTO;
import br.com.repassa.dto.PhotoFilterResponseDTO;
import br.com.repassa.repository.aws.PhotoProcessingRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class PhotoProcessingService {

    PhotoProcessingRepository photoProcessingRepository;

    @Inject
    public PhotoProcessingService(PhotoProcessingRepository photoProcessingRepository) {
        this.photoProcessingRepository = photoProcessingRepository;
    }

    public List<PhotoFilterResponseDTO> getPhotosProcessing(PhotoFilterDTO photoFilterDTO, String username, int pageSize, String lastEvaluatedKey) throws RepassaException {
        long inicio = System.currentTimeMillis();
        System.out.println("inicio da busca de fotos por usuário ");
        List<PhotoFilterResponseDTO> photoFilterResponseDTOS = this.photoProcessingRepository.listItensByDateAndUser_new(photoFilterDTO.getDate(), username, pageSize, lastEvaluatedKey);
        long fim = System.currentTimeMillis();
        System.out.printf("FIM da busca de fotos por usuário %.3f ms%n", (fim - inicio) / 1000d);
        return photoFilterResponseDTOS;
    }
}
