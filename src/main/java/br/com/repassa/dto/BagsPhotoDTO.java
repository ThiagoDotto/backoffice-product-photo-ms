package br.com.repassa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BagsPhotoDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3352685806899253677L;

    private LocalDateTime receiveDate;
    private LocalDateTime registrationDate;
    private String bagStatus;
    private String bagId;
    private String partner;
    private String clientEmail;
    private String items;
    private String photographyStatus;
}
