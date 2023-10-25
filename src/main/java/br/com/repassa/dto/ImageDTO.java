package br.com.repassa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageDTO {

    @NotNull
    private String groupId;

    private List<PhotoBase64DTO> photoBase64;

    @NotNull
    private String date;

}
