package br.com.repassa.service.rekognition;

import br.com.repassa.dto.IdentificatorsDTO;
import br.com.repassa.dto.ProcessBarCodeRequestDTO;
import br.com.repassa.enums.TypePhoto;
import br.com.repassa.resource.client.RekognitionBarClient;
import br.com.repassa.utils.StringUtils;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class RekognitionService {


    public List<IdentificatorsDTO> PhotosRecognition(List<ProcessBarCodeRequestDTO.GroupPhoto> groupPhotos) {
        RekognitionClient rekognitionClient = new RekognitionBarClient().openConnection();
        List<IdentificatorsDTO> validateIds = new ArrayList<>();

        groupPhotos.forEach(item ->
                item.getPhotos().forEach(photo -> {
                    if (Objects.equals(photo.getTypePhoto(), TypePhoto.ETIQUETA)) {
                        String url = photo.getUrlPhotoBarCode();
                        String bucket = url.split("\\.")[0].replace("https://", "");
                        String pathImage = url.split("\\.com/")[1].replace("+", " ");

                        DetectTextRequest decReq = DetectTextRequest.builder()
                                .image(Image.builder()
                                        .s3Object(S3Object.builder()
                                                .bucket(bucket)
                                                .name(pathImage)
                                                .build())
                                        .build())
                                .build();

                        DetectTextResponse decRes = rekognitionClient.detectText(decReq);

                        boolean foundText = false;
                        for (TextDetection textDetection : decRes.textDetections()) {
                            String productId = StringUtils.extractNumber(textDetection.detectedText());

                            validateIds.add(IdentificatorsDTO.builder()
                                    .groupId(item.getId())
                                    .productId(productId)
                                    .build());
                            foundText = true;
                            break;
                        }

                        if (!foundText) {
                            validateIds.add(IdentificatorsDTO.builder()
                                    .groupId(item.getId())
                                    .productId(null)
                                    .valid(false)
                                    .build());
                        }
                    }
                }));

        rekognitionClient.close();
        return validateIds;
    }
}
