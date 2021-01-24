package com.laboschqpa.filehost.model.inputstream;

import java.io.InputStream;

public interface CountingInputStreamInterface {

    /**
     * @return Total amount of bytes read by this stream.
     */
    long getCountOfReadBytes();

    /**
     * @return The underlying InputStream
     */
    InputStream getInputStream();
}
