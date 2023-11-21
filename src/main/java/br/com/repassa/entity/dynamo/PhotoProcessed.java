package br.com.repassa.entity.dynamo;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PhotoProcessed {

    private String id;
    private String bagId;
    private String editedBy;
    private String imageId;
    private String isValid;
    private String originalImageUrl;
    private String sizePhoto;
    private String imageName;
    private String urlThumbnail;
    private String uploadDate;
}
