package br.com.repassa.dto;

import br.com.repassa.enums.TypePhoto;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeleteGroupPhotosDTO implements Serializable {

    @NotNull(message = "O id do PhotoManager é obrigatório.")
    private String id;

    @NotNull(message = "Lista dos grupos é obrigatório.")
    private List<GroupPhoto> groups;

    @Getter
    @Setter
    public static class GroupPhoto {
        @NotNull(message = "O id do Grupo é obrigatório.")
        private String id;
    }
}