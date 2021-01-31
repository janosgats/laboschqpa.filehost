package com.laboschqpa.filehost.model.buffer;

/**
 * Non-threadsafe implementation.
 */
public class AutoResizingMemoryDuplexByteStore implements DuplexByteStore {
    private byte[] buffer;
    private double autoResizingFactor;
    private boolean closed = false;

    public AutoResizingMemoryDuplexByteStore(int initialBufferSize, double autoResizingFactor) {
        if (autoResizingFactor <= 1.0000001) {
            throw new UnsupportedOperationException("resizingFactor has to be at least 1.0000001!");
        }

        buffer = new byte[initialBufferSize];
        this.autoResizingFactor = autoResizingFactor;
    }

    @Override
    public int getCurrentBufferSize() {
        assertNotClosed();
        return buffer.length;
    }

    @Override
    public void forceResize(int newSize) {
        assertNotClosed();
        resize(newSize);
    }

    @Override
    public byte read(int position) {
        assertNotClosed();
        return buffer[position];
    }

    @Override
    public void write(int position, byte b) {
        assertNotClosed();
        if (position >= buffer.length) {
            resize(getNextAutoSize(position));
        }

        buffer[position] = b;
    }

    @Override
    public void close() {
        closed = true;
        buffer = null;
    }

    protected int getNextAutoSize(int positionToWriteTo) {
        return Math.max(positionToWriteTo + 1, (int) (getCurrentBufferSize() * autoResizingFactor) + 1);
    }

    protected void resize(int newSize) {
        final byte[] newBuffer = new byte[newSize];

        for (int i = 0; i < buffer.length && i < newBuffer.length; ++i) {
            newBuffer[i] = buffer[i];
        }
        buffer = newBuffer;
    }

    void assertNotClosed() {
        if (closed) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " is closed!");
        }
    }
}
