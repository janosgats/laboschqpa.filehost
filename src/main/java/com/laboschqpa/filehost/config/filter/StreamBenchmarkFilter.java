package com.laboschqpa.filehost.config.filter;

import com.laboschqpa.filehost.model.inputstream.TrackingInputStream;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStreamFactory;
import com.laboschqpa.filehost.util.ServletHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Log4j2
@Component
@Order(2)
@RequiredArgsConstructor
public class StreamBenchmarkFilter implements Filter {
    private static final int EOF = -1;

    private final TrackingInputStreamFactory trackingInputStreamFactory;

    private boolean enableStreamBenchmarkFilter;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        if (enableStreamBenchmarkFilter && httpServletRequest.getRequestURI().contains("/benchmark/streamspeed")) {
            doBenchmark(httpServletRequest, httpServletResponse);
        } else {
            chain.doFilter(request, response);
        }
    }

    private void doBenchmark(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        log.warn("Doing stream benchmark on URL {}", httpServletRequest.getRequestURI());


        final ServletFileUpload servletFileUpload = new ServletFileUpload();

        FileItemIterator iterator = servletFileUpload.getItemIterator(httpServletRequest);

        byte[] readBuff = new byte[2000000];

        long readStartTime = System.nanoTime();

        long countOfReadBytes = 0;
        while (iterator.hasNext()) {
            final InputStream partInputStream = iterator.next().openStream();
            final TrackingInputStream trackingInputStream = trackingInputStreamFactory.createForFileUpload(partInputStream);

            while (trackingInputStream.read(readBuff) != EOF) ;

            trackingInputStream.close();
            countOfReadBytes += trackingInputStream.getCountOfReadBytes();
        }

        long readEndTime = System.nanoTime();


        double countOfReadMegaBytes = countOfReadBytes / (double) (1000 * 1000);

        long readTimeNanos = readEndTime - readStartTime;
        double readTimeSeconds = readTimeNanos / (double) (1000 * 1000 * 1000);

        Map<String, String> benchmarkResult = Map.of(
                "result", "success",
                "countOfReadBytes", String.valueOf(countOfReadBytes),
                "countOfReadMegaBytes", String.valueOf(countOfReadMegaBytes),
                "readTimeNanos", String.valueOf(readTimeNanos),
                "readTimeSeconds", String.valueOf(readTimeSeconds),
                "speedMBPerSec", String.valueOf(countOfReadMegaBytes / readTimeSeconds)
        );
        log.info("Stream Benchmark Result: {}", benchmarkResult);
        ServletHelper.setJsonResponse(httpServletResponse, benchmarkResult, 200);
    }

    @Value("${streambenchmarkfilter.enable:false}")
    public void setEnableStreamBenchmarkFilter(Boolean enableStreamBenchmarkFilter) {
        this.enableStreamBenchmarkFilter = enableStreamBenchmarkFilter;
    }
}