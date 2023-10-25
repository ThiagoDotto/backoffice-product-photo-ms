package br.com.repassa.dto;

import lombok.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoFilterResponseDTO implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -2646405401205267054L;
    private String id;
    private String bagId;
    private String editedBy;
    private String imageId;
    private String isValid;
    private String originalImageUrl;
    private String sizePhoto;
    private String imageName;
    private String thumbnailBase64;
    private String uploadDate;


    public static PhotoFilterResponseDTO from(Map<String, AttributeValue> item) {
        PhotoFilterResponseDTO photoFilterResponseDTO = new PhotoFilterResponseDTO();
        if (item != null && !item.isEmpty()) {
            String imageName = item.get("imagem_name").s();

            if (imageName == null) {
                imageName = "undefined";
            }
            photoFilterResponseDTO.setBagId(item.get("bag_id").s());
            photoFilterResponseDTO.setEditedBy(item.get("edited_by").s());
            photoFilterResponseDTO.setImageName(imageName);
            photoFilterResponseDTO.setId(item.get("id").s());
            photoFilterResponseDTO.setImageId(item.get("image_id").s());
            photoFilterResponseDTO.setIsValid(item.get("is_valid").s());
            photoFilterResponseDTO.setOriginalImageUrl(item.get("original_image_url").s());
            photoFilterResponseDTO.setSizePhoto(item.get("size_photo").s());
            photoFilterResponseDTO.setThumbnailBase64(item.get("thumbnail_base64").s());
            photoFilterResponseDTO.setUploadDate(item.get("upload_date").s());
//            photoFilterResponseDTO.setId((item.get(PhotoAbstractService.FRUIT_NAME_COL).s());
//            photosManager.setDescription(item.get(AbstractService.FRUIT_DESC_COL).s());
        }
        return photoFilterResponseDTO;
    }
}
