package br.com.repassa.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
@Getter
@Setter
@Builder
public class CompositionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 8369861489657747451L;

    private Long compositionId;
    private Integer compositionValue;
}
