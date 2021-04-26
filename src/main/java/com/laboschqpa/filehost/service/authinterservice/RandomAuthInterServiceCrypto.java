package com.laboschqpa.filehost.service.authinterservice;

import com.google.common.primitives.Longs;
import com.laboschqpa.filehost.exceptions.AuthInterServiceException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

@Log4j2
@Service
public class RandomAuthInterServiceCrypto implements AuthInterServiceCrypto {
    private static final String HEADER_SEPARATOR = "_";
    private static final String SIGNATURE_METHOD = "HmacSHA512";
    static final long ALLOWED_TIME_DIFFERENCE_SECONDS = 30;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getEncoder();
    private static final Base64.Decoder base64Decoder = Base64.getDecoder();

    private final String authInterServiceManualKey;
    private final boolean enableManualKey;
    private final SecretKeySpec secretKeySpec;

    public RandomAuthInterServiceCrypto(@Value("${auth.interservice.key}") String authInterServiceKey,
                                        @Value("${auth.interservice.manualKey:null}") String authInterServiceManualKey,
                                        @Value("${auth.interservice.enableManualKey:false}") boolean enableManualKey) {
        if (StringUtils.isBlank(authInterServiceKey)) {
            throw new AuthInterServiceException("AuthInterService key is blank!");
        }

        this.authInterServiceManualKey = authInterServiceManualKey;
        this.enableManualKey = enableManualKey;

        this.secretKeySpec = new SecretKeySpec(authInterServiceKey.getBytes(StandardCharsets.UTF_8), SIGNATURE_METHOD);
        getNewMac();//To get an exception in the constructor if MACs cannot be created with the current config & environment
    }

    /**
     * @param authInterServiceHeader value of the received AuthInterService header
     * @return {@code true} if the header is valid, {@code false} if not
     */
    @Override
    public boolean isHeaderValid(final String authInterServiceHeader) {
        if (StringUtils.isBlank(authInterServiceHeader)) {
            return false;
        }
        if (isHeaderValidForManualKey(authInterServiceHeader)) {
            return true;
        }

        final String[] splitHeader = authInterServiceHeader.split(HEADER_SEPARATOR);
        if (splitHeader.length != 3) {
            return false;
        }

        final byte[] receivedEpochSeconds = base64Decoder.decode(splitHeader[0]);
        final byte[] receivedMessage = base64Decoder.decode(splitHeader[1]);
        final byte[] receivedHmac = base64Decoder.decode(splitHeader[2]);

        final Instant receivedInstant = Instant.ofEpochSecond(Longs.fromByteArray(receivedEpochSeconds));
        final Instant now = getInstantNow();
        if (receivedInstant.isBefore(now.minusSeconds(ALLOWED_TIME_DIFFERENCE_SECONDS))
                || receivedInstant.isAfter(now.plusSeconds(ALLOWED_TIME_DIFFERENCE_SECONDS))) {
            log.warn("AuthInterService header is invalid because of time difference. receives: {}, now: {}",
                    receivedInstant, now);
            return false;
        }

        final byte[] receivedBytesToHash = concatBytesToHash(receivedEpochSeconds, receivedMessage);
        return Arrays.equals(receivedHmac, calculateHmac(receivedBytesToHash));
    }

    boolean isHeaderValidForManualKey(final String authInterServiceHeader) {
        if (!enableManualKey) {
            return false;
        }
        if (StringUtils.isBlank(authInterServiceManualKey)) {
            throw new AuthInterServiceException("The configured authInterServiceManualKey is blank when tried to use it to auth!");
        }


        if (StringUtils.isBlank(authInterServiceHeader)) {
            return false;
        }
        return authInterServiceManualKey.equals(authInterServiceHeader);
    }

    /**
     * Generates HMAC of a random sequence, not of the HTTP request.
     */
    @Override
    public String generateHeader() {
        final byte[] epochSecondsBytes = Longs.toByteArray(getInstantNow().getEpochSecond());

        final byte[] messageBytes = new byte[128];
        secureRandom.nextBytes(messageBytes);

        final byte[] toHashBytes = concatBytesToHash(epochSecondsBytes, messageBytes);
        final byte[] hmac = calculateHmac(toHashBytes);

        return base64Encoder.encodeToString(epochSecondsBytes) + HEADER_SEPARATOR
                + base64Encoder.encodeToString(messageBytes) + HEADER_SEPARATOR
                + base64Encoder.encodeToString(hmac);
    }


    byte[] concatBytesToHash(byte[] epochSecondsBytes, byte[] messageBytes) {
        final byte[] toHashBytes = new byte[epochSecondsBytes.length + messageBytes.length];
        System.arraycopy(epochSecondsBytes, 0, toHashBytes, 0, epochSecondsBytes.length);
        System.arraycopy(messageBytes, 0, toHashBytes, epochSecondsBytes.length, messageBytes.length);

        return toHashBytes;
    }

    byte[] calculateHmac(byte[] message) {
        return getNewMac().doFinal(message);
    }

    Mac getNewMac() {
        try {
            final Mac mac = Mac.getInstance(SIGNATURE_METHOD);
            mac.init(secretKeySpec);
            return mac;
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new AuthInterServiceException("Cannot init HMAC generator", e);
        }
    }

    /**
     * To mock the current time in tests by Spies
     */
    Instant getInstantNow() {
        return Instant.now();
    }
}
