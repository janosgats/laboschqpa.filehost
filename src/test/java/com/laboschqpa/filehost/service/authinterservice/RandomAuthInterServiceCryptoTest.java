package com.laboschqpa.filehost.service.authinterservice;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class RandomAuthInterServiceCryptoTest {
    final static String AUTH_INTER_SERVICE_KEY1 = "testKey1";
    final static String AUTH_INTER_SERVICE_KEY2 = "testKey2";

    final static String AUTH_INTER_SERVICE_MANUAL_KEY = "testManualKey";

    RandomAuthInterServiceCrypto randomAuthInterServiceCrypto1;
    RandomAuthInterServiceCrypto randomAuthInterServiceCrypto2;

    @BeforeEach
    void beforeEach() {
        randomAuthInterServiceCrypto1 = spy(new RandomAuthInterServiceCrypto(AUTH_INTER_SERVICE_KEY1, AUTH_INTER_SERVICE_MANUAL_KEY, true));
        randomAuthInterServiceCrypto2 = spy(new RandomAuthInterServiceCrypto(AUTH_INTER_SERVICE_KEY2, AUTH_INTER_SERVICE_MANUAL_KEY, true));
    }

    @Test
    void generateAndCrossValidateHeader() {
        final Instant now = Instant.now();
        doReturn(now).when(randomAuthInterServiceCrypto1).getInstantNow();
        doReturn(now).when(randomAuthInterServiceCrypto2).getInstantNow();

        final String generatedHeader1 = randomAuthInterServiceCrypto1.generateHeader();
        final String generatedHeader2 = randomAuthInterServiceCrypto2.generateHeader();

        assertTrue(StringUtils.isNotBlank(generatedHeader1));
        assertTrue(StringUtils.isNotBlank(generatedHeader2));

        assertTrue(randomAuthInterServiceCrypto1.isHeaderValid(generatedHeader1));
        assertFalse(randomAuthInterServiceCrypto1.isHeaderValid(generatedHeader2));

        assertFalse(randomAuthInterServiceCrypto2.isHeaderValid(generatedHeader1));
        assertTrue(randomAuthInterServiceCrypto2.isHeaderValid(generatedHeader2));
    }

    @Test
    void timeoutMakesHeaderInvalid() {
        final Instant now = Instant.now();
        doReturn(now).when(randomAuthInterServiceCrypto1).getInstantNow();

        final String generatedHeader1 = randomAuthInterServiceCrypto1.generateHeader();

        assertTrue(randomAuthInterServiceCrypto1.isHeaderValid(generatedHeader1));

        doReturn(now.plusSeconds(RandomAuthInterServiceCrypto.ALLOWED_TIME_DIFFERENCE_SECONDS * 2))
                .when(randomAuthInterServiceCrypto1).getInstantNow();
        assertFalse(randomAuthInterServiceCrypto1.isHeaderValid(generatedHeader1));

        doReturn(now.minusSeconds(RandomAuthInterServiceCrypto.ALLOWED_TIME_DIFFERENCE_SECONDS * 2))
                .when(randomAuthInterServiceCrypto1).getInstantNow();
        assertFalse(randomAuthInterServiceCrypto1.isHeaderValid(generatedHeader1));

        doReturn(now.plusSeconds(RandomAuthInterServiceCrypto.ALLOWED_TIME_DIFFERENCE_SECONDS / 2))
                .when(randomAuthInterServiceCrypto1).getInstantNow();
        assertTrue(randomAuthInterServiceCrypto1.isHeaderValid(generatedHeader1));

        doReturn(now.minusSeconds(RandomAuthInterServiceCrypto.ALLOWED_TIME_DIFFERENCE_SECONDS / 2))
                .when(randomAuthInterServiceCrypto1).getInstantNow();
        assertTrue(randomAuthInterServiceCrypto1.isHeaderValid(generatedHeader1));
    }

    @Test
    void isHeaderValid_blankHeader() {
        assertFalse(randomAuthInterServiceCrypto1.isHeaderValid(null));
        assertFalse(randomAuthInterServiceCrypto1.isHeaderValid(""));
    }

    @Test
    void isHeaderValid_manualKey() {
        RandomAuthInterServiceCrypto cryptoEnabled = spy(new RandomAuthInterServiceCrypto(AUTH_INTER_SERVICE_KEY1, AUTH_INTER_SERVICE_MANUAL_KEY, true));
        RandomAuthInterServiceCrypto cryptoDisabled = spy(new RandomAuthInterServiceCrypto(AUTH_INTER_SERVICE_KEY1, AUTH_INTER_SERVICE_MANUAL_KEY, false));

        assertTrue(cryptoEnabled.isHeaderValid(AUTH_INTER_SERVICE_MANUAL_KEY));
        assertFalse(cryptoEnabled.isHeaderValid("asdasd"));

        assertFalse(cryptoDisabled.isHeaderValid(AUTH_INTER_SERVICE_MANUAL_KEY));
    }
}