package br.com.repassa.dto;

import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhotoBase64DTO {

    @NotNull
    private String type;

    @NotNull
    private String base64;

    @NotNull
    private String name;
    private String size;
    private String note;
    private String urlThumbNail;

}
