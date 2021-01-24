package com.laboschqpa.filehost.model.inputstream;

import java.io.InputStream;

public abstract class CountingInputStream extends InputStream {

    /**
     * @return Total amount of bytes read by this stream.
     */
    abstract public long getCountOfReadBytes();
}
