package br.com.repassa.service.rekognition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import br.com.repassa.dto.IdentificatorsDTO;
import br.com.repassa.dto.ProcessBarCodeRequestDTO;
import br.com.repassa.enums.TypePhoto;
import br.com.repassa.resource.client.RekognitionBarClient;
import br.com.repassa.repository.aws.PhotoProcessingRepository;
import br.com.repassa.utils.ProcessBarCodeThreadUtils;
import br.com.repassa.utils.StringUtils;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectTextRequest;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.TextDetection;

@ApplicationScoped
public class RekognitionService {

    @Inject
    PhotoProcessingRepository photoProcessingRepository;

    public List<IdentificatorsDTO> PhotosRecognitionThread(List<ProcessBarCodeRequestDTO.GroupPhoto> groupPhotos) {
        int numberOfItems = groupPhotos.size(); // Substitua pelo número real de itens a serem processados
        int optimalThreadCount = calculateOptimalThreadCount(numberOfItems, Runtime.getRuntime().availableProcessors());
        int itemsPerThread = numberOfItems / optimalThreadCount;

        List<String> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(optimalThreadCount);
        List<IdentificatorsDTO> validateIds = new ArrayList<>();

        for (int i = 0; i < optimalThreadCount; i++) {
            int start = i * itemsPerThread + 1;
            int end = (i + 1) * itemsPerThread;

            // Para a última thread, ajuste o 'end' para garantir que todos os itens sejam cobertos
            if (i == optimalThreadCount - 1) {
                end = numberOfItems;
            }

            Runnable processBarCodeThreadUtils = new ProcessBarCodeThreadUtils(start, end, results, latch);
            Thread thread = new Thread(processBarCodeThreadUtils);
            thread.start();
        }

        // Aguarda o término de todas as threads
        try {
            latch.await(); // Aguarda até que todas as threads tenham concluído
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Agora 'results' contém todos os códigos retornados pelas threads
        System.out.println("Resultados: " + results);

        return validateIds;
    }

    public List<IdentificatorsDTO> PhotosRecognition(List<ProcessBarCodeRequestDTO.GroupPhoto> groupPhotos) {
        RekognitionClient rekognitionClient = new RekognitionBarClient().openConnection();
        List<IdentificatorsDTO> validateIds = new ArrayList<>();

        groupPhotos.forEach(item -> item.getPhotos().forEach(photo -> {
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

    private static int calculateOptimalThreadCount(int numberOfItems, int availableProcessors) {
        // Ajuste o fator conforme necessário
        double loadFactor = 0.8;
        int optimalThreadCount = (int) (availableProcessors / (1 - loadFactor));

        // Certifique-se de que o número de threads não exceda o número de itens
        return Math.min(optimalThreadCount, numberOfItems);
    }
}
