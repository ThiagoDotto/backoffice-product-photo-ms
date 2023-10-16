package br.com.repassa.dto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeTypePhotoDTO implements Serializable {
    @NotNull(message = "O campo photoId é obrigatório.")
    private String photoId;
    @NotNull(message = "O campo groupId é obrigatório.")
    private String groupId;
    @NotNull(message = "O campo typePhoto é obrigatório.")
    private String typePhoto;
    private String message;
}