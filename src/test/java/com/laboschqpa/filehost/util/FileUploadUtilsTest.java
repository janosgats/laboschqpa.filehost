package com.laboschqpa.filehost.util;

import org.apache.tomcat.util.http.fileupload.FileItemHeaders;
import org.apache.tomcat.util.http.fileupload.FileItemStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileUploadUtilsTest {

    @Test
    void createNormalizedFileName() {
        FileItemStream fileItemStream = createFileItemStreamWhichReturnsName("x/x/y.z");
        String result = FileUploadUtils.createNormalizedFileName(fileItemStream);
        assertEquals("y.z", result);

        fileItemStream = createFileItemStreamWhichReturnsName("x\\x\\y.z");
        result = FileUploadUtils.createNormalizedFileName(fileItemStream);
        assertEquals("y.z", result);

        fileItemStream = createFileItemStreamWhichReturnsName("x/x\\x/x\\y.z");
        result = FileUploadUtils.createNormalizedFileName(fileItemStream);
        assertEquals("y.z", result);

        fileItemStream = createFileItemStreamWhichReturnsName("  y.z  ");
        result = FileUploadUtils.createNormalizedFileName(fileItemStream);
        assertEquals("y.z", result);
    }

    private FileItemStream createFileItemStreamWhichReturnsName(final String name) {
        return new FileItemStream() {
            @Override
            public InputStream openStream() throws IOException {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getFieldName() {
                return null;
            }

            @Override
            public boolean isFormField() {
                return false;
            }

            @Override
            public FileItemHeaders getHeaders() {
                return null;
            }

            @Override
            public void setHeaders(FileItemHeaders headers) {

            }
        };
    }
}