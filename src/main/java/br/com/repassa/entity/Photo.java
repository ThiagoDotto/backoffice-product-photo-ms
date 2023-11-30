package br.com.repassa.entity;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.repassa.enums.TypePhoto;
import lombok.*;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Photo implements Serializable {

    @Serial
    private static final long serialVersionUID = -8254079600074018535L;

    private String id;
    private String urlPhoto;
    private String namePhoto;
    private String sizePhoto;
    private String urlThumbnail;
    private TypePhoto typePhoto;
    @Builder.Default
    private String note = null;

}
