package com.laboschqpa.filehost;

import com.laboschqpa.filehost.exceptions.apierrordescriptor.StreamLengthLimitExceededException;
import com.laboschqpa.filehost.model.inputstream.TrackingInputStream;
import com.laboschqpa.filehost.model.streamtracking.StreamTracker;
import com.laboschqpa.filehost.model.streamtracking.StreamTrackerImpl;
import com.laboschqpa.filehost.model.streamtracking.TrackedIntervalStateFormatters;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Ignore
@ExtendWith(MockitoExtension.class)
public class S3 {
    private static final String BUCKET_NAME = "laboschqpa";

    static S3Presigner s3Presigner;
    static S3Client s3Client;

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        final String accessKeyId = System.getenv("TEMP_SCALEWAY_ACCESSKEY_ID");
        final String accessKeySecret = System.getenv("TEMP_SCALEWAY_ACCESSKEY_SECRET");

        s3Client = S3Client.builder()
                .region(Region.of("pl-waw"))
                .credentialsProvider(() -> AwsBasicCredentials.create(accessKeyId, accessKeySecret))
                .endpointOverride(new URI("https://s3.pl-waw.scw.cloud"))
                .build();

        s3Presigner = S3Presigner.builder()
                .region(Region.of("pl-waw"))
                .credentialsProvider(() -> AwsBasicCredentials.create(accessKeyId, accessKeySecret))
                .endpointOverride(new URI("https://s3.pl-waw.scw.cloud"))
                .build();
    }

    @AfterAll
    static void afterAll() {
        s3Client.close();
        s3Presigner.close();
    }

    @Test
    void listFilesInBucket() {
        final List<S3Object> s3Objects = s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET_NAME).build()).contents();

        System.out.println("Objects in bucket: " + s3Objects.stream().map(S3Object::key).collect(Collectors.joining(",")));
    }

    @Test
    void createPresignedUrl() {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(BUCKET_NAME).key("TestUpload2").build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest =
                s3Presigner.presignGetObject(getObjectPresignRequest);

        System.out.println("Pre-signed URL: " + presignedGetObjectRequest.url());
    }

    static class ListFiller implements Runnable {
        private LinkedList<Byte> linkedList;
        private long length;

        public ListFiller(LinkedList<Byte> linkedList, long length) {
            this.linkedList = linkedList;
            this.length = length;
        }

        @Override
        public void run() {
            fillList(linkedList);
            System.out.println("ListFiller run() finished.");
        }

        void fillList(LinkedList<Byte> linkedList) {
            final long chunkSize = 100000;
            final long originalStartTime = System.nanoTime();
            long startTime = originalStartTime;

            long count = 0;

            while (count < length) {

                for (int i = 0; i < chunkSize && count < length; ++i) {
                    linkedList.addLast((byte) ((count % 16) + 65));
                    ++count;
                }

                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                final long nt = System.nanoTime();
                final long chunkTime = nt - startTime;
                startTime = nt;
                System.out.println("Chunk pushed into list. " +
                        "speed: " + (((chunkSize * 1000000000) / chunkTime) / 1000) + "KB/s" +
                        " total pushed: " + count / (double) 1000 + "KB" +
                        " current size: " + linkedList.size() / (double) 1000 + "KB");
            }

            final long allTime = System.nanoTime() - originalStartTime;
            System.out.println("Total time: " + (allTime) / 1000000 + "ms Avg speed: " + (((count * 1000000000) / allTime) / 1000) + "KB/s");
        }
    }

    Pair<InputStream, Thread> startInputStream(long length) {
        final long waitingForFillTimeout = 1000;

        final LinkedList<Byte> linkedList = new LinkedList<>();

        ListFiller listFiller = new ListFiller(linkedList, length);
        Thread listFillerThread = new Thread(listFiller);

        final InputStream inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                Byte popped;
                long waitedTime = 0;
                while ((popped = linkedList.poll()) == null && waitedTime < waitingForFillTimeout) {
                    try {
                        long l = 1;
                        waitedTime += l;
                        Thread.sleep(l);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (waitedTime > 0)
                    System.out.println("Waiting for fill (" + waitedTime + " ms)");

                if (popped == null) {
                    return -1;
                }
                return popped;
            }
        };

        listFillerThread.start();
        return Pair.of(inputStream, listFillerThread);
    }

    @Test
    void putObject() throws InterruptedException {
        final long contentLength = 2 * 1000 * 1000;
        final Pair<InputStream, Thread> pair = startInputStream(contentLength);
        InputStream inputStream = pair.getLeft();
        Thread inputFillerThread = pair.getRight();

        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key("TestUpload6")
                .contentType("image/jpg")
                .build();

        final RequestBody requestBody = RequestBody.fromInputStream(inputStream, contentLength);
        System.out.println("##################################### BEFORE PUT OBJECT #####################################");
        System.out.println("received ETag: " + s3Client.putObject(putObjectRequest, requestBody).eTag());
        System.out.println("##################################### AFTER PUT OBJECT #####################################");
        inputFillerThread.join();
        System.out.println("##################################### AFTER THREAD JOIN #####################################");
    }

    @Test
    void putObjectFromFile() throws InterruptedException, FileNotFoundException {
        File file = new File("D:\\Filmek\\Rick and Morty\\Rick.and.Morty.S01.1080p.BluRay.REMUX.VC-1.TrueHD.5.1.Eng.Hun-MrDeta\\Rick.and.Morty.S01E01.Pilot.mkv");
        final long contentLength = file.length();
        StreamTracker tracker = new StreamTrackerImpl("str name", TrackedIntervalStateFormatters::formatAllGbPerSecSpeedMb);
        TrackingInputStream inputStream = new TrackingInputStream(new FileInputStream(file), tracker);
        inputStream.setLimit(300 * 1000 * 1000);
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 200; ++i) {
                System.out.println("Tracked absolute: " + tracker.popAndFormatTrackingIntervalState());
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    break;
                }
            }
        });
        thread.start();

        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key("TestUpload6")
                .contentType("image/jpg")
                .build();

        final RequestBody requestBody = RequestBody.fromInputStream(inputStream, -1);
        System.out.println("##################################### BEFORE PUT OBJECT #####################################");
        try {
            System.out.println("received ETag: " + s3Client.putObject(putObjectRequest, requestBody).eTag());
        } catch (StreamLengthLimitExceededException e) {
            System.out.println("StreamLengthLimitExceededException: " + e.getMessage());
        }
        System.out.println("##################################### AFTER PUT OBJECT #####################################");
        thread.interrupt();
        thread.join();
        System.out.println("##################################### AFTER THREAD JOIN #####################################");
    }

    @Test
    void putObjectMultipart() throws InterruptedException, IOException {
//        byte[] toUpload = new byte[27 * 1000 * 1000];
//        StreamTracker tracker = new StreamTrackerImpl("str name", TrackedIntervalStateFormatters::formatAllGbPerSecSpeedMb);
//        TrackingInputStream inputStream = new TrackingInputStream(new ByteArrayInputStream(toUpload), tracker);
        File file = new File("D:\\Users\\gatsj\\Downloads\\python-3.9.1-amd64.exe");
        final long contentLength = file.length();
        StreamTracker tracker = new StreamTrackerImpl("str name", TrackedIntervalStateFormatters::formatAllGbPerSecSpeedMb);
        TrackingInputStream inputStream = new TrackingInputStream(new FileInputStream(file), tracker);
        inputStream.setLimit(100 * 1000 * 1000);
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 200; ++i) {
                System.out.println("Tracked absolute: " + tracker.popAndFormatTrackingIntervalState());
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println("Tracked absolute: " + tracker.popAndFormatTrackingIntervalState());
                    break;
                }
            }
        });
        thread.start();


        System.out.println("##################################### BEFORE PUT OBJECT #####################################");
        try {
            final String objectKey = "TestUpload8";
            final int partSize = 6 * 1000 * 1000;

            // First create a multipart upload and get the upload id
            CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(objectKey)
                    .build();

            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createMultipartUploadRequest);
            final String uploadId = response.uploadId();
            System.out.println("Multipart upload ID: " + uploadId);

            final List<CompletedPart> completedParts = new ArrayList<>();

//            byte[] uploadBuffer = new byte[partSize];
            for (int i = 1; i < 2; ++i) {
//                int countOfReadBytes = inputStream.readNBytes(uploadBuffer, 0, partSize);
//                if (countOfReadBytes == 0) {
//                    break;
//                }

                // Upload all the different parts of the object
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(objectKey)
                        .uploadId(uploadId)
                        .partNumber(i).build();

//                System.out.println("Uploading part " + i + " with size: " + countOfReadBytes);
//                UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest,
//                        RequestBody.fromInputStream(new ByteArrayInputStream(uploadBuffer), countOfReadBytes));
                UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest,
                        RequestBody.fromInputStream(inputStream, contentLength));
                completedParts.add(CompletedPart.builder().partNumber(i).eTag(uploadPartResponse.eTag()).build());
            }

            // Finally call completeMultipartUpload operation to tell S3 to merge all uploaded
            // parts and finish the multipart operation.
            CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();

            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    CompleteMultipartUploadRequest.builder()
                            .bucket(BUCKET_NAME)
                            .key(objectKey)
                            .uploadId(uploadId)
                            .multipartUpload(completedMultipartUpload)
                            .build();

            s3Client.completeMultipartUpload(completeMultipartUploadRequest);

        } catch (StreamLengthLimitExceededException e) {
            System.out.println("StreamLengthLimitExceededException: " + e.getMessage());
        }
        System.out.println("##################################### AFTER PUT OBJECT #####################################");
        thread.interrupt();
        thread.join();
        System.out.println("##################################### AFTER THREAD JOIN #####################################");
    }
}
