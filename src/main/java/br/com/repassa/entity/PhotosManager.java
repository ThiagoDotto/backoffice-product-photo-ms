package br.com.repassa.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.repassa.enums.StatusManagerPhotos;
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
public class PhotosManager implements Serializable {

    @Serial
    private static final long serialVersionUID = -1011698530635714137L;

    private String id;
    private String editor;
    private String date;
    private StatusManagerPhotos statusManagerPhotos;
    private List<GroupPhotos> groupPhotos;
}
