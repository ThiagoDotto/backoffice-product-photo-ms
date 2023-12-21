package br.com.repassa.dto;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDTO {
    @NotNull
    private String groupId;

    private List<PhotoBase64DTO> photoBase64;

    @NotNull
    private String date;

    private Integer size;
}
