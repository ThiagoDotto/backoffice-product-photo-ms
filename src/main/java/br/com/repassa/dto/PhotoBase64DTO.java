package br.com.repassa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
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

}
