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
public class IdentificatorsDTO implements Serializable {
    @NotNull(message = "O campo productId é obrigatório.")
    private String productId;
    @NotNull(message = "O campo groupId é obrigatório.")
    private String groupId;
    private Boolean valid = false;
    private String message = "";
}