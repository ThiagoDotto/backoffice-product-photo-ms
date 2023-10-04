package br.com.repassa.service.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import br.com.repassa.service.enums.TypeError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupPhotos implements Serializable {

    @Serial
    private static final long serialVersionUID = -3869326221476646938L;

    private int id;
    private Long productId;
    private TypeError typeError;
    private List<Photo> photos;
}
