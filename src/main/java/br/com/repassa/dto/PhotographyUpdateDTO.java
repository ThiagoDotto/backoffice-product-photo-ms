package br.com.repassa.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serial;
import java.io.Serializable;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhotographyUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -4716175537936483407L;
    private Long bagId;
    private String photographyStatus;
    private String photographyUpdateDate;
    private String photographyFinishedQty;
}