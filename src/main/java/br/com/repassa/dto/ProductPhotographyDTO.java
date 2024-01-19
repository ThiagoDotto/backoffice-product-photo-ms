package br.com.repassa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductPhotographyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 8380965813829152891L;

    private Long bagId;
    private Long productId;
    private String mainPhoto;
    private String backPhoto;
    private String detailPhoto;
    private String status;
    private String editorEmail;
    private String updatedAt;
}
