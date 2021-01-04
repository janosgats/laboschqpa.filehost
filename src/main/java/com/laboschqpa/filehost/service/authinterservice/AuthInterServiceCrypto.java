package com.laboschqpa.filehost.service.authinterservice;

public interface AuthInterServiceCrypto {
    boolean isHeaderValid(String authInterServiceHeader);

    String generateHeader();
}
