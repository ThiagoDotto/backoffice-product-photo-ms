package br.com.repassa.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
public class MeasureDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -5424390035026388752L;

    private Long measureId;
    private Integer measureValue;
}
