package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.model.file.UploadableFile;
import com.laboschqpa.filehost.model.inputstream.CountingInputStream;
import com.laboschqpa.filehost.model.upload.FileUploadRequest;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.service.imagevariant.VariantCreatorService;
import com.laboschqpa.filehost.util.fileuploadconfigurer.AnyFileUploadConfigurer;
import com.laboschqpa.filehost.util.fileuploadconfigurer.FileUploadConfigurerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploaderServiceTest {
    static Detector tikaDetector = TikaConfig.getDefaultConfig().getDetector();
    @Mock
    IndexedFileEntityRepository indexedFileEntityRepository;
    @Mock
    AnyFileUploadConfigurer anyFileUploadConfigurer;
    @Mock
    FileUploadConfigurerFactory fileUploadConfigurerFactory;
    @Mock
    VariantCreatorService variantCreatorService;

    FileUploaderService fileUploaderService;

    @BeforeEach
    void beforeEach() {
        when(fileUploadConfigurerFactory.get(any()))
                .thenReturn(anyFileUploadConfigurer);

        fileUploaderService = new FileUploaderService(
                fileUploadConfigurerFactory,
                variantCreatorService,
                null,
                indexedFileEntityRepository,
                null,
                null,
                tikaDetector
        );
    }

    /**
     * Testing for multiple stream lengths because the stream joining depends on:
     * <ul>
     *     <li>the amount read by tika</li>
     *     <li>the memory buffer size</li>
     *     <li>etc...</li>
     * </ul>
     */
    private static List<Integer> provideArgumentsFor_detectMimeTypeAndPersist_testForUndamagedStream() {
        return List.of(
                0,
                10,
                1000,
                100 * 1000,
                10 * 1000 * 1000
        );
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsFor_detectMimeTypeAndPersist_testForUndamagedStream")
    void detectMimeTypeAndPersist_testForUndamagedStream(int streamLength) {
        final byte[] testStreamContent = new byte[streamLength];
        for (int i = 0; i < streamLength; ++i) {
            testStreamContent[i] = (byte) (i % 256);
        }
        final CountingInputStream countingInputStream = new CountingInputStream(new ByteArrayInputStream(testStreamContent));

        final IndexedFileEntity indexedFileEntity = new IndexedFileEntity(17L);
        final UploadableFile uploadableFile = mock(UploadableFile.class);

        final ByteArrayOutputStream testReadOutputStream = new LoadingByteArrayOutputStream();

        when(uploadableFile.getEntity())
                .thenReturn(indexedFileEntity);

        doAnswer((Answer<Void>) invocation -> {
            InputStream inputStream = invocation.getArgument(0);
            IOUtils.copyLarge(inputStream, testReadOutputStream);
            return null;
        }).when(uploadableFile).saveFromStream(any());

        fileUploaderService.detectMimeTypeAndPersist(new FileUploadRequest(), countingInputStream, uploadableFile);

        byte[] actualCopiedOutputStreamArray = testReadOutputStream.toByteArray();
        assertEquals(streamLength, actualCopiedOutputStreamArray.length);
        assertArrayEquals(testStreamContent, actualCopiedOutputStreamArray);
    }
}