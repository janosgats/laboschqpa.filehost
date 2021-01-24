package com.laboschqpa.filehost.model.inputstream;

import java.io.*;


/**
 * A copy of Apache's {@link org.apache.tika.utils.RereadableInputStream} but it's better in three things:
 * <ul>
 *     <li>releases the memory buffer on close</li>
 *     <li>does not return the unfilled default zero bytes from the memory buffer on reads after rewind but returns EOF</li>
 *     <li>does not count EOF bytes as valid read bytes from the original stream</li>
 * </ul>
 * <br>
 * The original implementation has "too private" fields so I had to copy the class's code.
 */
public class MemoryReleasingRereadableInputStream extends InputStream {
    private static final int EOF = -1;

    /**
     * Input stream originally passed to the constructor.
     */
    private InputStream originalInputStream;

    /**
     * The inputStream currently being used by this object to read contents;
     * may be the original stream passed in, or a stream that reads
     * the saved copy.
     */
    private InputStream inputStream;

    /**
     * Maximum number of bytes that can be stored in memory before
     * storage will be moved to a temporary file.
     */
    private int maxBytesInMemory;

    /**
     * True when the original stream is being read; set to false when
     * reading is set to use the stored data instead.
     */
    private boolean firstPass = true;

    /**
     * Whether or not the stream's contents are being stored in a file
     * as opposed to memory.
     */
    private boolean bufferIsInFile;

    /**
     * The buffer used to store the stream's content; this storage is moved
     * to a file when the stored data's size exceeds maxBytesInMemory.
     */
    private byte[] byteBuffer;

    /**
     * The total number of bytes read from the original stream at the time.
     */
    private int bytesReadFromOriginalStream;

    /**
     * File used to store the stream's contents; is null until the stored
     * content's size exceeds maxBytesInMemory.
     */
    private File storeFile;

    /**
     * OutputStream used to save the content of the input stream in a
     * temporary file.
     */
    private OutputStream storeOutputStream;


    /**
     * Specifies whether or not to read to the end of stream on first
     * rewind.  This defaults to true.  If this is set to false,
     * then the first time when rewind() is called, only those bytes
     * already read from the original stream will be available from then on.
     */
    private boolean readToEndOfStreamOnFirstRewind = true;


    /**
     * Specifies whether or not to close the original input stream
     * when close() is called.  Defaults to true.
     */
    private boolean closeOriginalStreamOnClose = true;


    /**
     * Creates a rereadable input stream.
     *
     * @param inputStream                    stream containing the source of data
     * @param maxBytesInMemory               maximum number of bytes to use to store
     *                                       the stream's contents in memory before switching to disk; note that
     *                                       the instance will preallocate a byte array whose size is
     *                                       maxBytesInMemory.  This byte array will be made available for
     *                                       garbage collection (i.e. its reference set to null) when the
     *                                       content size exceeds the array's size, when close() is called, or
     *                                       when there are no more references to the instance.
     * @param readToEndOfStreamOnFirstRewind Specifies whether or not to
     *                                       read to the end of stream on first rewind.  If this is set to false,
     *                                       then when rewind() is first called, only those bytes already read
     *                                       from the original stream will be available from then on.
     */
    public MemoryReleasingRereadableInputStream(InputStream inputStream, int maxBytesInMemory,
                                                boolean readToEndOfStreamOnFirstRewind,
                                                boolean closeOriginalStreamOnClose) {
        this.inputStream = inputStream;
        this.originalInputStream = inputStream;
        this.maxBytesInMemory = maxBytesInMemory;
        byteBuffer = new byte[maxBytesInMemory];
        this.readToEndOfStreamOnFirstRewind = readToEndOfStreamOnFirstRewind;
        this.closeOriginalStreamOnClose = closeOriginalStreamOnClose;
    }

    /**
     * Reads a byte from the stream, saving it in the store if it is being
     * read from the original stream.  Implements the abstract
     * InputStream.read().
     *
     * @return the read byte, or -1 on end of stream.
     * @throws IOException
     */
    public int read() throws IOException {
        int inputByte = inputStream.read();
        if (inputByte != EOF && firstPass) {
            saveByte(inputByte);
        }
        return inputByte;
    }

    /**
     * "Rewinds" the stream to the beginning for rereading.
     *
     * @throws IOException
     */
    public void rewind() throws IOException {

        if (firstPass && readToEndOfStreamOnFirstRewind) {
            // Force read to end of stream to fill store with any
            // remaining bytes from original stream.
            while (read() != EOF) {
                // empty loop
            }
        }

        closeStream();
        if (storeOutputStream != null) {
            storeOutputStream.close();
            storeOutputStream = null;
        }
        firstPass = false;
        boolean newStreamIsInMemory = (bytesReadFromOriginalStream < maxBytesInMemory);
        inputStream = newStreamIsInMemory
                ? new ByteArrayInputStream(byteBuffer, 0, bytesReadFromOriginalStream)
                : new BufferedInputStream(new FileInputStream(storeFile));
    }

    /**
     * Closes the input stream currently used for reading (may either be
     * the original stream or a memory or file stream after the first pass).
     *
     * @throws IOException
     */
    // Does anyone need/want for this to be public?
    private void closeStream() throws IOException {
        if (inputStream != null
                &&
                (inputStream != originalInputStream
                        || closeOriginalStreamOnClose)) {
            inputStream.close();
            inputStream = null;
        }
    }

    /**
     * Closes the input stream and removes the temporary file if one was
     * created.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        closeStream();

        if (storeOutputStream != null) {
            storeOutputStream.close();
            storeOutputStream = null;
        }

        super.close();
        byteBuffer = null;// release for garbage collection
        if (storeFile != null) {
            storeFile.delete();
        }
    }

    /**
     * Returns the number of bytes read from the original stream.
     *
     * @return number of bytes read
     */
    public int getBytesReadFromOriginalStream() {
        return bytesReadFromOriginalStream;
    }

    /**
     * Saves the byte read from the original stream to the store.
     *
     * @param inputByte byte read from original stream
     * @throws IOException
     */
    private void saveByte(int inputByte) throws IOException {

        if (!bufferIsInFile) {
            boolean switchToFile = (bytesReadFromOriginalStream == (maxBytesInMemory));
            if (switchToFile) {
                storeFile = File.createTempFile("TIKA_streamstore_", ".tmp");
                bufferIsInFile = true;
                storeOutputStream = new BufferedOutputStream(
                        new FileOutputStream(storeFile));
                storeOutputStream.write(byteBuffer, 0, bytesReadFromOriginalStream);
                storeOutputStream.write(inputByte);
                byteBuffer = null; // release for garbage collection
            } else {
                byteBuffer[bytesReadFromOriginalStream] = (byte) inputByte;
            }
        } else {
            storeOutputStream.write(inputByte);
        }
        ++bytesReadFromOriginalStream;
    }
}
