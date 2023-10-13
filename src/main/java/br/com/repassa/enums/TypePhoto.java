package br.com.repassa.enums;

public enum TypePhoto {


    ETIQUETA,
    PRINCIPAL,
    COSTA,
    DETALHE;

    public static TypePhoto getPosition(int count) {
        return TypePhoto.values()[count];
    }
}
