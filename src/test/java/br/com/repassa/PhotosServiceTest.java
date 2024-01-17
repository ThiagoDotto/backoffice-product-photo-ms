package br.com.repassa;


import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import br.com.repassa.config.AwsConfig;
import br.com.repassa.dto.PhotoBagsResponseDTO;
import br.com.repassa.dto.PhotoFilterDTO;
import br.com.repassa.dto.PhotoFilterResponseDTO;
import br.com.repassa.dto.history.BagsResponseDTO;
import br.com.repassa.dto.history.HistoryResponseDTO;
import br.com.repassa.entity.GroupPhotos;
import br.com.repassa.entity.Photo;
import br.com.repassa.entity.PhotosManager;
import br.com.repassa.enums.StatusProduct;
import br.com.repassa.enums.TypePhoto;
import br.com.repassa.repository.aws.PhotoManagerRepository;
import br.com.repassa.resource.client.AwsS3Client;
import br.com.repassa.resource.client.PhotoClientInterface;
import br.com.repassa.service.HistoryService;
import br.com.repassa.service.PhotosService;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.*;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


public class PhotosServiceTest {
    @InjectMocks
    PhotosService photosService;

    @Mock
    AwsConfig awsConfig;
    @Mock
    private PhotoManagerRepository photoManagerRepository = mock(PhotoManagerRepository.class);

    @Mock
    AwsS3Client awsS3Client;

    @Mock
    HistoryService historyService;

    List<PhotoFilterResponseDTO> listPhotoFilter = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        this.listPhotoFilter = createListPhotoFilter();
    }

    @Test
    void testFilterAndPersist() throws RepassaException {
        // Arrange
        PhotoFilterDTO filter = new PhotoFilterDTO("2023-09-22");
        String fieldFiltered = "id, bag_id, edited_by, image_id, imagem_name, is_valid, original_image_url, size_photo, url_thumbnail, upload_date";
        String username = "Daniel Oliveira";
        Map<String, Object> expressionAttributeValues = new HashMap<String, Object>();

        expressionAttributeValues.put(":upload_date", filter.getDate());
        expressionAttributeValues.put(":edited_by", username);

        PhotoClientInterface photoClient = mock(PhotoClientInterface.class);

        when(photoClient.listItem(fieldFiltered, expressionAttributeValues)).thenReturn(listPhotoFilter);
    }


    @Test
    void shouldGetPhotosByProductIdWithSuccess() throws Exception {
        final var productId = "10203040";

        final var photo = Photo.builder()
                .id("id")
                .namePhoto("name")
                .urlPhoto("url")
                .typePhoto(TypePhoto.PRINCIPAL)
                .sizePhoto("size")
                .build();

        final var groupPhoto = GroupPhotos.builder()
                .photos(List.of(photo))
                .productId("10203040")
                .statusProduct(StatusProduct.FINISHED)
                .build();

        final var productManager = PhotosManager.builder()
                .groupPhotos(List.of(groupPhoto))
                .build();

        when(photoManagerRepository.findByProductId(anyString())).thenReturn(productManager);

        final var actual = photosService.findPhotoByProductId(productId);

        assertNotNull(actual);
        assertEquals(1, actual.getPhotos().size());
        assertEquals("PRINCIPAL", actual.getPhotos().get(0).getTypePhoto());
    }

    @Test
    void shouldNotGetPhotosByProductIdWhenNotFinished() throws Exception {
        final var productId = "10203040";

        final var photo = Photo.builder()
                .id("id")
                .namePhoto("name")
                .urlPhoto("url")
                .typePhoto(TypePhoto.PRINCIPAL)
                .sizePhoto("size")
                .build();

        final var groupPhoto = GroupPhotos.builder()
                .photos(List.of(photo))
                .statusProduct(StatusProduct.IN_PROGRESS)
                .build();

        final var productManager = PhotosManager.builder()
                .groupPhotos(List.of(groupPhoto))
                .build();

        when(photoManagerRepository.findByProductId(anyString())).thenReturn(productManager);

        final var actual = photosService.findPhotoByProductId(productId);

        assertNotNull(actual);
        assertEquals(0, actual.getPhotos().size());
    }

    @Test
    void shouldReturnEmptyListWhenProductIdNotFound() throws Exception {
        final var productId = "10203040";

        when(photoManagerRepository.findByProductId(anyString())).thenReturn(null);

        final var actual = photosService.findPhotoByProductId(productId);

        assertEquals(0, actual.getPhotos().size());
    }

    @Test
    void shouldThrowRepassaExceptionWhenDynamoError() throws Exception {
        final var productId = "10203040";

        when(photoManagerRepository.findByProductId(anyString())).thenThrow(DynamoDbException.class);

        assertThrows(RepassaException.class, () -> photosService.findPhotoByProductId(productId));
    }

//    @Test
//    void shouldInsertImage_returnOk() throws RepassaException {
//
//        var photoValidate = new PhotosValidate();
//        var base64 = "data:image/jpeg;base64,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";
//        var photoBase64 = PhotoBase64DTO.builder().base64(base64).type("jpeg")
//                .size("1000").name("teste").build();
//        var imageDTO = ImageDTO.builder().groupId("eb489fdd-f1d3-4678-801b-21a8a911e89b")
//                .photoBase64(Arrays.asList(photoBase64))
//                .date(LocalDateTime.now().toString())
//                .build();
//        var objectKey = imageDTO.getPhotoBase64().get(0).getName() + "." + imageDTO.getPhotoBase64().get(0).getType();
//        var cloudFront = "https://assets-dev-curadoria.repassa.com.br";
//        var name = "teste";
//        var bucketName = "backoffice-triage-photo-dev";
//        var urlImage = "";
//
//        doNothing().when(photoValidate).validatePhotos(any());
//        when(photoValidate.validatePathBucket(anyString(), anyString())).thenReturn(objectKey);
////        when(awsS3Client.uploadBase64FileToS3(anyString(), anyString(), anyString())).thenReturn(cloudFront + objectKey);
//        doNothing().when(this.photosService).savePhotoProcessingDynamo(any(), anyString(), any());
//
//        var photosManager = photosService.insertImage(imageDTO, name);
//
//
//        assertNotNull(photosManager);
//    }

    private List<PhotoFilterResponseDTO> createListPhotoFilter() {

        PhotoFilterResponseDTO filterResponseDTO = new PhotoFilterResponseDTO();
        filterResponseDTO.setBagId("6ba3176b-fbc1-4a80-8f6f-4209661bd741");
        filterResponseDTO.setEditedBy("daniel+oliveira");
        filterResponseDTO.setImageName("BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setImageId("97d40106-21b4-465b-966b-d1ef9f542626");
        filterResponseDTO.setIsValid("true");
        filterResponseDTO.setOriginalImageUrl(
                "https://backoffice-triage-photo-dev.s3.amazonaws.com/fotografia/daniel+oliveira/2023-09-20/BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setSizePhoto("12746487");
        filterResponseDTO.setUrlThumbnail(
                "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC");
        filterResponseDTO.setUploadDate("2023-09-22T14:29:03.553Z");

        PhotoFilterResponseDTO filterResponseDTO1 = new PhotoFilterResponseDTO();
        filterResponseDTO.setBagId("6ba3176b-fbc1-4a80-8f6f-4209661bd741");
        filterResponseDTO.setEditedBy("daniel+oliveira");
        filterResponseDTO.setImageName("BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setImageId("97d40106-21b4-465b-966b-d1ef9f542626");
        filterResponseDTO.setIsValid("true");
        filterResponseDTO.setOriginalImageUrl(
                "https://backoffice-triage-photo-dev.s3.amazonaws.com/fotografia/daniel+oliveira/2023-09-20/BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setSizePhoto("12746487");
        filterResponseDTO.setUrlThumbnail(
                "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC");
        filterResponseDTO.setUploadDate("2023-09-22T14:29:03.553Z");

        PhotoFilterResponseDTO filterResponseDTO2 = new PhotoFilterResponseDTO();
        filterResponseDTO.setBagId("6ba3176b-fbc1-4a80-8f6f-4209661bd741");
        filterResponseDTO.setEditedBy("daniel+oliveira");
        filterResponseDTO.setImageName("BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setImageId("97d40106-21b4-465b-966b-d1ef9f542626");
        filterResponseDTO.setIsValid("true");
        filterResponseDTO.setOriginalImageUrl(
                "https://backoffice-triage-photo-dev.s3.amazonaws.com/fotografia/daniel+oliveira/2023-09-20/BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setSizePhoto("12746487");
        filterResponseDTO.setUrlThumbnail(
                "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC");
        filterResponseDTO.setUploadDate("2023-09-22T14:29:03.553Z");

        PhotoFilterResponseDTO filterResponseDTO3 = new PhotoFilterResponseDTO();
        filterResponseDTO.setBagId("6ba3176b-fbc1-4a80-8f6f-4209661bd741");
        filterResponseDTO.setEditedBy("daniel+oliveira");
        filterResponseDTO.setImageName("BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setImageId("97d40106-21b4-465b-966b-d1ef9f542626");
        filterResponseDTO.setIsValid("true");
        filterResponseDTO.setOriginalImageUrl(
                "https://backoffice-triage-photo-dev.s3.amazonaws.com/fotografia/daniel+oliveira/2023-09-20/BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setSizePhoto("12746487");
        filterResponseDTO.setUrlThumbnail(
                "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC");
        filterResponseDTO.setUploadDate("2023-09-22T14:29:03.553Z");

        PhotoFilterResponseDTO filterResponseDTO4 = new PhotoFilterResponseDTO();
        filterResponseDTO.setBagId("6ba3176b-fbc1-4a80-8f6f-4209661bd741");
        filterResponseDTO.setEditedBy("daniel+oliveira");
        filterResponseDTO.setImageName("BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setImageId("97d40106-21b4-465b-966b-d1ef9f542626");
        filterResponseDTO.setIsValid("true");
        filterResponseDTO.setOriginalImageUrl(
                "https://backoffice-triage-photo-dev.s3.amazonaws.com/fotografia/daniel+oliveira/2023-09-20/BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setSizePhoto("12746487");
        filterResponseDTO.setUrlThumbnail(
                "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC");
        filterResponseDTO.setUploadDate("2023-09-22T14:29:03.553Z");

        PhotoFilterResponseDTO filterResponseDTO5 = new PhotoFilterResponseDTO();
        filterResponseDTO.setBagId("6ba3176b-fbc1-4a80-8f6f-4209661bd741");
        filterResponseDTO.setEditedBy("daniel+oliveira");
        filterResponseDTO.setImageName("BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setImageId("97d40106-21b4-465b-966b-d1ef9f542626");
        filterResponseDTO.setIsValid("true");
        filterResponseDTO.setOriginalImageUrl(
                "https://backoffice-triage-photo-dev.s3.amazonaws.com/fotografia/daniel+oliveira/2023-09-20/BlackMarble_2016_1400m_africa_m_labeled.jpg");
        filterResponseDTO.setSizePhoto("12746487");
        filterResponseDTO.setUrlThumbnail(
                "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC");
        filterResponseDTO.setUploadDate("2023-09-22T14:29:03.553Z");

        this.listPhotoFilter.addAll(Arrays.asList(filterResponseDTO, filterResponseDTO1,
                filterResponseDTO2, filterResponseDTO3, filterResponseDTO4, filterResponseDTO5));

        return listPhotoFilter;
    }

    @Test
    void testFindBagsForPhoto() throws RepassaException {
        int page = 1;
        int size = 10;
        String bagId = "123";
        String email = "test@example.com";
        String statusBag = "OPEN";
        String partner = "SomePartner";
        String photographyStatus = "APPROVED";

        BagsResponseDTO bagsResponseDTO = new BagsResponseDTO(); // Suponha que você precise configurar corretamente os valores aqui
        HistoryResponseDTO historyResponseDTO = new HistoryResponseDTO(BigInteger.TEN, Collections.singletonList(bagsResponseDTO));

        when(historyService.findHistorys(eq(page), eq(size), eq(bagId), eq(email), eq(statusBag), eq(partner), eq(photographyStatus), eq("MS-PHOTO")))
                .thenReturn(historyResponseDTO);

        PhotoBagsResponseDTO result = photosService.findBagsForPhoto(page, size, bagId, email, statusBag, partner, photographyStatus);

        assertEquals(BigInteger.TEN, result.getTotalRecords());
        assertEquals(1, result.getBagsProductDTO().size());


        Mockito.verify(historyService, Mockito.times(1)).findHistorys(eq(page), eq(size), eq(bagId), eq(email), eq(statusBag), eq(partner), eq(photographyStatus), eq("MS-PHOTO"));
    }

    @Test
    void testFindBagsForPhotoWithException() throws RepassaException {
        int page = 1;
        int size = 10;
        String bagId = "123";
        String email = "test@example.com";
        String statusBag = "OPEN";
        String partner = "SomePartner";
        String photographyStatus = "APPROVED";

        when(historyService.findHistorys(eq(page), eq(size), eq(bagId), eq(email), eq(statusBag), eq(partner), eq(photographyStatus), eq("MS-PHOTO")))
                .thenThrow(new ClientWebApplicationException());

        assertThrows(RepassaException.class,
                () -> photosService.findBagsForPhoto(page, size, bagId, email, statusBag, partner, photographyStatus));

        Mockito.verify(historyService, Mockito.times(1)).findHistorys(eq(page), eq(size), eq(bagId), eq(email), eq(statusBag), eq(partner), eq(photographyStatus), eq("MS-PHOTO"));
    }
}
