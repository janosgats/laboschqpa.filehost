package com.laboschqpa.filehost.model.buffer;

import java.io.Closeable;

/**
 * A byte collection writeable and readable at the specified positions.
 * All writes are persisted (e.g. flushed, written, etc...) before reads (they are always visible in reads).
 * <br>
 * This can be implemented by a simple array buffer, an IOStream buffered file buffer, etc...
 */
public interface DuplexByteStore extends AutoCloseable, Closeable {

    int getCurrentBufferSize();

    void forceResize(int newSize);

    byte read(int position);

    void write(int position, byte b);
}
//TODO: Add a bulk read and bulk write based on array buffers