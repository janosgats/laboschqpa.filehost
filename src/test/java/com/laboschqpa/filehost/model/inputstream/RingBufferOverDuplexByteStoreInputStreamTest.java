package com.laboschqpa.filehost.model.inputstream;

import com.laboschqpa.filehost.model.buffer.AutoResizingMemoryDuplexByteStore;
import com.laboschqpa.filehost.model.buffer.DuplexByteStore;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class RingBufferOverDuplexByteStoreInputStreamTest {
    static final int EOF = -1;

    @Test
    void testIfStreamIsNotDamaged() throws IOException {
        DuplexByteStore duplexByteStore;

        duplexByteStore = new AutoResizingMemoryDuplexByteStore(1, 1.5);
        testForUndamagedStream_readOne(10, 100, true, duplexByteStore);
        testForUndamagedStream_readBulk(10, 100, true, duplexByteStore);

        duplexByteStore = new AutoResizingMemoryDuplexByteStore(3, 3);
        testForUndamagedStream_readOne(10, 12, true, duplexByteStore);
        testForUndamagedStream_readBulk(10, 12, true, duplexByteStore);

        duplexByteStore = new AutoResizingMemoryDuplexByteStore(10, 1.5);
        testForUndamagedStream_readOne(10, 100, true, duplexByteStore);
        testForUndamagedStream_readBulk(10, 100, true, duplexByteStore);

        duplexByteStore = new AutoResizingMemoryDuplexByteStore(1, 1.5);
        testForUndamagedStream_readOne(100, 10, false, duplexByteStore);
        testForUndamagedStream_readBulk(100, 10, false, duplexByteStore);

        duplexByteStore = new AutoResizingMemoryDuplexByteStore(0, 2);
        testForUndamagedStream_readOne(100, 10, false, duplexByteStore);
        testForUndamagedStream_readBulk(100, 10, false, duplexByteStore);

        duplexByteStore = new AutoResizingMemoryDuplexByteStore(3, 1.4);
        testForUndamagedStream_readOne(500, 600, true, duplexByteStore);
        testForUndamagedStream_readBulk(500, 600, true, duplexByteStore);

        duplexByteStore = new AutoResizingMemoryDuplexByteStore(3000, 3);
        testForUndamagedStream_readOne(10000, 12000, true, duplexByteStore);
        testForUndamagedStream_readBulk(10000, 12000, true, duplexByteStore);

        duplexByteStore = new AutoResizingMemoryDuplexByteStore(5000, 3);
        testForUndamagedStream_readOne(3000, 1000, false, duplexByteStore);
        testForUndamagedStream_readBulk(3000, 1000, false, duplexByteStore);
    }

    void testForUndamagedStream_readOne(int streamLength, int ringSize, boolean shouldReachEof, DuplexByteStore duplexByteStore) throws IOException {
        final byte[] testStreamContent = new byte[streamLength];
        for (int i = 0; i < streamLength; ++i) {
            testStreamContent[i] = (byte) (i % 256);
        }
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(testStreamContent));

        final RingBufferOverDuplexByteStoreInputStream ringBufferInputStream
                = new RingBufferOverDuplexByteStoreInputStream(inputStream, duplexByteStore, ringSize, 4096);

        assertEquals(shouldReachEof, ringBufferInputStream.readUntilRingIsFullOrEndOfStreamReached());

        final ByteArrayOutputStream testReadOutputStream = new LoadingByteArrayOutputStream();

        while (true) {
            int readByte = ringBufferInputStream.read();
            if (readByte == EOF)
                break;
            testReadOutputStream.write(readByte);
        }

        byte[] actualCopiedOutputStreamArray = testReadOutputStream.toByteArray();
        assertEquals(streamLength, actualCopiedOutputStreamArray.length);
        assertArrayEquals(testStreamContent, actualCopiedOutputStreamArray);
    }

    void testForUndamagedStream_readBulk(int streamLength, int ringSize, boolean shouldReachEof, DuplexByteStore duplexByteStore) throws IOException {
        final byte[] testStreamContent = new byte[streamLength];
        for (int i = 0; i < streamLength; ++i) {
            testStreamContent[i] = (byte) (i % 256);
        }
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(testStreamContent));

        final RingBufferOverDuplexByteStoreInputStream ringBufferInputStream
                = new RingBufferOverDuplexByteStoreInputStream(inputStream, duplexByteStore, ringSize, 4096);

        assertEquals(shouldReachEof, ringBufferInputStream.readUntilRingIsFullOrEndOfStreamReached());

        final ByteArrayOutputStream testReadOutputStream = new LoadingByteArrayOutputStream();
        IOUtils.copyLarge(ringBufferInputStream, testReadOutputStream);


        byte[] actualCopiedOutputStreamArray = testReadOutputStream.toByteArray();
        assertEquals(streamLength, actualCopiedOutputStreamArray.length);
        assertArrayEquals(testStreamContent, actualCopiedOutputStreamArray);
    }
}