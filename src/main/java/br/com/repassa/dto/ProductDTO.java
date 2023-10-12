package br.com.repassa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3181298267724142L;

    private Long bagId;
    private String title;
    private String productId;
    private String userCreatedEmail;
    private String userCreatedId;
    private String dateCreatedAt;
    private String userUpdatedEmail;
    private String userUpdatedId;
    private String dateUpdatedAt;
    private String registrationStatus;
    private String status;
    private Long genderId;
    private Long categoryId;
    private Long subcategoryId;
    private Long modelId;
    private Long clothingLengthId;
    private Long styleId;
    private Long productSizeId;
    private List<MeasureDTO> measureList;
    private List<MaterialDTO> materialList;
    private List<CompositionDTO> compositionList;
    private List<AdditionalInfoUpdateDTO> additionalInfoUpdateDTOList;
    private Long eyeglassLensColorId;
    private Long colorId;
    private Long toneId;
    private String sellingPrice;
    private String originalPrice;
    private Long usageStatusId;
}