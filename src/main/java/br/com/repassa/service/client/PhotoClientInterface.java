package br.com.repassa.service.client;

import java.util.List;
import java.util.Map;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.service.dto.PhotoFilterResponseDTO;

public interface PhotoClientInterface {
    public List<PhotoFilterResponseDTO> listItem(String fieldFiltered,
                                                 Map<String, Object> expressionAttributeValues) throws RepassaException;
}
