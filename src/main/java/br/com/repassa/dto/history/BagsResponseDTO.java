package br.com.repassa.dto.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BagsResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1825981948649941689L;

    private BigInteger bagId;
    private String receivedDate;
    private String triageDate;
    private String partner;
    private String email;
    private String userId;
    private String userName;
    private String clientName;
    private String clientEmail;
    private String statusBag;
    private String identifier;
    private String item;
    private String qtyItem;
    private String qtyApprovedItem;
    private String qtyDisapprovedItem;

}
