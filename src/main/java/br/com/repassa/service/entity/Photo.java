package br.com.repassa.service.entity;

import java.io.Serial;
import java.io.Serializable;

import br.com.repassa.service.enums.TypePhoto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Photo implements Serializable {

    @Serial
    private static final long serialVersionUID = -8254079600074018535L;

    private String id;
    private String urlPhoto;
    private String namePhoto;
    private String sizePhoto;
    private String base64;
    private TypePhoto typePhoto;

}
