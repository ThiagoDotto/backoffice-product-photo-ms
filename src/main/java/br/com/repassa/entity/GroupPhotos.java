package br.com.repassa.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.repassa.enums.StatusProduct;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupPhotos implements Serializable {

    @Serial
    private static final long serialVersionUID = -3869326221476646938L;

    private String id;
    private String productId;
    private String imageError;
    private String idError;
    private StatusProduct statusProduct;
    private List<Photo> photos;
    private String updateDate;
}
