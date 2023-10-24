package br.com.repassa.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
public class ProductPhotoListDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5205914803407006540L;

    private List<ProductPhotoDTO> photos;
}
