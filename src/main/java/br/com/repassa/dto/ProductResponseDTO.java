package br.com.repassa.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class ProductResponseDTO implements Serializable {
    public static final String PHOTOMANAGER_FINISHED = "As imagens associadas a esta data foram todas concluídas com sucesso!";

    private String message;
}
