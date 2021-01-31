package com.laboschqpa.filehost.model.inputstream;

import com.laboschqpa.filehost.model.buffer.DuplexByteStore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class RingBufferOverDuplexByteStoreInputStream extends InputStream {
    private static final int EOF = -1;
    private static byte[] readBuffer;

    private boolean closed = false;

    final DuplexByteStore byteStore;
    private final int ringSizePlusOne; // Using a ByteStore 1 byte longer than the ring size to differentiate between an empty and a full ring.

    private int lastReadPosition;
    private int nextWritePosition = 0;
    private InputStream inputStream;

    public RingBufferOverDuplexByteStoreInputStream(InputStream inputStream, DuplexByteStore byteStore, int ringSize, int readBufferSize) {
        if (ringSize <= 0) {
            throw new UnsupportedOperationException("ringSize has to be a positive number but " + ringSize + " was given!");
        }
        if (readBufferSize <= 0) {
            throw new UnsupportedOperationException("readBufferSize has to be a positive number but " + readBufferSize + " was given!");
        }

        this.inputStream = inputStream;
        this.byteStore = byteStore;
        this.ringSizePlusOne = ringSize + 1;
        lastReadPosition = ringSizePlusOne - 1;
        readBuffer = new byte[readBufferSize];
    }

    @Override
    public int read() throws IOException {
        assertNotClosed();

        if (!isThereAByteToRead()) {
            if (!isThereSpaceToWrite()) {
                throw new IllegalStateException("Cannot read and cannot write the ring!");
            }

            final boolean wasEofReached = copyChunkFromInputStreamIntoRing();
            if (wasEofReached) {
                return EOF;
            }
        }

        final byte byteFromByteStore = unsafeReadNextFromByteStore();
        return signedByteToUnsigned(byteFromByteStore);//We need to convert it into int, because -1 byte value would mean EOF
    }

    @Override
    public int read(byte[] bufferToFill, int off, int len) throws IOException {
        assertNotClosed();
        Objects.checkFromIndexSize(off, len, bufferToFill.length);

        boolean wasEofReached = false;
        while (getCurrentUnreadSize() < len && isThereSpaceToWrite() && !wasEofReached) {
            wasEofReached = copyChunkFromInputStreamIntoRing();
        }

        if (wasEofReached && !isThereAByteToRead()) {
            return EOF;
        }

        int reallyReadBytes = 0;
        for (int i = off; i < off + len && isThereAByteToRead(); ++i) {
            bufferToFill[i] = unsafeReadNextFromByteStore();
            ++reallyReadBytes;
        }

        return reallyReadBytes;
    }

    /**
     * @return {@code True} if {@code EOF} is reached, {@code false} if the ring is full.
     */
    public boolean readUntilRingIsFullOrEndOfStreamReached() throws IOException {
        assertNotClosed();

        while (isThereSpaceToWrite()) {
            final boolean wasEofReached = copyChunkFromInputStreamIntoRing();
            if (wasEofReached) {
                return true;
            }
        }

        return false;
    }


    /**
     * @return {@code True} if {@code EOF} is reached and the function did not read any bytes, {@code false} otherwise.
     */
    boolean copyChunkFromInputStreamIntoRing() throws IOException {
        if (!isThereSpaceToWrite()) {
            return false;
        }

        final int bytesToRead = Math.min(getRemainingSpaceToWriteSize(), readBuffer.length);

        int reallyReadBytesCount = inputStream.read(readBuffer, 0, bytesToRead);
        if (reallyReadBytesCount == EOF) {
            return true;
        }

        if (getRemainingSpaceToWriteSize() < reallyReadBytesCount) {
            throw new IllegalStateException("More bytes were read than space left in the ring." +
                    " read: " + reallyReadBytesCount + ", space left: " + getRemainingSpaceToWriteSize());
        }

        for (int i = 0; i < reallyReadBytesCount; ++i) {
            unsafeWriteNextToByteStore(readBuffer[i]);
        }

        return false;
    }

    /**
     * @return The current amount of bytes that are buffered in the ring and was not read yet.
     */
    public final int getCurrentUnreadSize() {
        return (ringSizePlusOne - 1 - lastReadPosition + nextWritePosition) % ringSizePlusOne;
    }

    /**
     * @return The free space in the ring that can be filled from the wrapped inputStream.
     */
    public final int getRemainingSpaceToWriteSize() {
        return ringSizePlusOne - getCurrentUnreadSize() - 1;
    }

    @Override
    public void close() throws IOException {
        closed = true;

        readBuffer = null;
        byteStore.close();
        inputStream.close();
    }

    final int signedByteToUnsigned(byte b) {
        return b & 0xFF;
    }

    final byte unsafeReadNextFromByteStore() {
        byte b = byteStore.read(getIncrementedReadPosition());
        incrementReadPosition();
        return b;
    }

    final void unsafeWriteNextToByteStore(byte b) {
        byteStore.write(nextWritePosition, b);
        incrementWritePosition();
    }

    final boolean isThereSpaceToWrite() {
        return getRemainingSpaceToWriteSize() > 0;
    }

    final boolean isThereAByteToRead() {
        return getCurrentUnreadSize() > 0;
    }

    final void incrementWritePosition() {
        nextWritePosition = getIncrementedWritePosition();
    }

    final void incrementReadPosition() {
        lastReadPosition = getIncrementedReadPosition();
    }

    final int getIncrementedReadPosition() {
        if (lastReadPosition >= ringSizePlusOne - 1)
            return 0;
        else
            return lastReadPosition + 1;
    }

    final int getIncrementedWritePosition() {
        if (nextWritePosition >= ringSizePlusOne - 1)
            return 0;
        else
            return nextWritePosition + 1;
    }

    final void assertNotClosed() {
        if (closed) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " is closed!");
        }
    }
}
