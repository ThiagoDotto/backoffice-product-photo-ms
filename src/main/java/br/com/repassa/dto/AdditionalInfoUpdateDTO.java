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
public class AdditionalInfoUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5034007079695317440L;

    private Long additionalInfoId;
    private List<Long> additionalInfoValueIdList;
}
