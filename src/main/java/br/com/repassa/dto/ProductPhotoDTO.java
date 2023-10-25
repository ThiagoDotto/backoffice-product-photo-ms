package br.com.repassa.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
public class ProductPhotoDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6159232081158260167L;

    private String id;
    private String urlPhoto;
    private String namePhoto;
    private String sizePhoto;
    private String typePhoto;
}
