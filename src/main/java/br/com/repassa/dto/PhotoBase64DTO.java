package br.com.repassa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PhotoBase64DTO {

    private String type;
    private String base64;
    private String name;
    private String note;

}
