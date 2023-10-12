package br.com.repassa.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
public class MaterialDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6270322054920416144L;

    private Long materialId;
    private List<Long> materialValueIdList;
}
