package br.com.repassa.service;



import java.util.*;

import br.com.repassa.service.client.DynamoDbClient;
import br.com.repassa.service.client.PhotoClient;
import br.com.repassa.service.client.PhotoClientInterface;
import br.com.repassa.service.dto.PhotoFilterDTO;
import br.com.repassa.service.dto.PhotoFilterResponseDTO;
import br.com.repassa.service.repository.PhotosManagerRepository;
import br.com.repassa.service.service.PhotosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.backoffice_repassa_utils_lib.error.exception.RepassaException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;


public class PhotosServiceTest {
    @InjectMocks
    PhotosService photosService;

    @InjectMocks
    DynamoDbClient dynamoDbClient;

    @Mock
    private PhotoClient photoClient = mock(PhotoClient.class);

    @Mock
    PhotosManagerRepository photosManagerRepository;

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
        String fieldFiltered = "id, bag_id, edited_by, image_id, imagem_name, is_valid, original_image_url, size_photo, thumbnail_base64, upload_date";
        String username = "Daniel Oliveira";
        Map<String, Object> expressionAttributeValues = new HashMap<String, Object>();

        expressionAttributeValues.put(":upload_date", filter.getDate());
        expressionAttributeValues.put(":edited_by", username);

        PhotoClientInterface photoClient = mock(PhotoClientInterface.class);

        when(photoClient.listItem(fieldFiltered, expressionAttributeValues)).thenReturn(listPhotoFilter);
    }

    @Test
    void whenPersistPhotoManager() throws RepassaException {

        photosService.persistPhotoManager(listPhotoFilter);

    }

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
        filterResponseDTO.setThumbnailBase64(
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
        filterResponseDTO.setThumbnailBase64(
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
        filterResponseDTO.setThumbnailBase64(
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
        filterResponseDTO.setThumbnailBase64(
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
        filterResponseDTO.setThumbnailBase64(
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
        filterResponseDTO.setThumbnailBase64(
                "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC");
        filterResponseDTO.setUploadDate("2023-09-22T14:29:03.553Z");

        this.listPhotoFilter.addAll(Arrays.asList(filterResponseDTO, filterResponseDTO1,
                filterResponseDTO2, filterResponseDTO3, filterResponseDTO4, filterResponseDTO5));

        return listPhotoFilter;
    }
}
