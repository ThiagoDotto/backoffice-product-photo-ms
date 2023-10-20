package br.com.repassa.service;

import br.com.backoffice_repassa_utils_lib.dto.history.HistoryDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.PhotographyDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.StepDTO;
import br.com.backoffice_repassa_utils_lib.dto.history.enums.BagStatus;

public class HistoryManagement {


    public HistoryDTO addPhotography(String bagID, PhotographyDTO photographyDTO) {
        StepDTO stepDTO = StepDTO
                .builder()
                .photographs(photographyDTO)
                .build();

        return HistoryDTO.builder()
                .bagId(Long.parseLong(bagID))
                .stepDTO(stepDTO)
                .statusBag(BagStatus.PHOTOGRAPHY_REVIEW)
                .build();
    }
}
