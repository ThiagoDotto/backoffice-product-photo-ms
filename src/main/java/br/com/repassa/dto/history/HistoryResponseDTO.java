package br.com.repassa.dto.history;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoryResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 8267825357543676525L;

    private BigInteger totalRecords;
    private List<BagsResponseDTO> bagsResponse;
}

