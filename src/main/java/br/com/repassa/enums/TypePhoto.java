package br.com.repassa.enums;

public enum TypePhoto {
    ETIQUETA,
    PRINCIPAL,
    COSTAS,
    DETALHE;
    
    public static TypePhoto getPosition(int count) {
        return TypePhoto.values()[count];
    }
}
