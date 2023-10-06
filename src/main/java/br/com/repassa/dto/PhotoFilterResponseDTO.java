package br.com.repassa.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
