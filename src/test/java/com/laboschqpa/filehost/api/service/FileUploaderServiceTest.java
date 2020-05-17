package com.laboschqpa.filehost.api.service;

import org.apache.tomcat.util.http.fileupload.FileItemHeaders;
import org.apache.tomcat.util.http.fileupload.FileItemStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileUploaderServiceTest {

    @Spy
    @InjectMocks
    FileUploaderService fileUploaderService;

    @Test
    void normalizeUploadedFileName() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = FileUploaderService.class.getDeclaredMethod("normalizeUploadedFileName", FileItemStream.class);
        method.setAccessible(true);

        FileItemStream fileItemStream = createFileItemStreamWhichReturnsName("x/x/y.z");
        String result = (String) method.invoke(fileUploaderService, fileItemStream);
        assertEquals("y.z", result);

        fileItemStream = createFileItemStreamWhichReturnsName("x\\x\\y.z");
        result = (String) method.invoke(fileUploaderService, fileItemStream);
        assertEquals("y.z", result);

        fileItemStream = createFileItemStreamWhichReturnsName("x/x\\x/x\\y.z");
        result = (String) method.invoke(fileUploaderService, fileItemStream);
        assertEquals("y.z", result);

        fileItemStream = createFileItemStreamWhichReturnsName("  y.z  ");
        result = (String) method.invoke(fileUploaderService, fileItemStream);
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