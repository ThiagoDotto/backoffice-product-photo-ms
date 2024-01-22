package br.com.repassa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PhotoBagsResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3723699482506407834L;

    private BigInteger totalRecords;
    private List<BagsPhotoDTO> bagsProductDTO;
}
