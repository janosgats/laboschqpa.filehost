package com.laboschqpa.filehost.config.filter;

import com.laboschqpa.filehost.model.inputstream.TrackingInputStream;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStreamFactory;
import com.laboschqpa.filehost.util.ServletHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
@Order(2)
@RequiredArgsConstructor
public class StreamBenchmarkFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(StreamBenchmarkFilter.class);

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

    private void doBenchmark(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        logger.warn("Doing stream benchmark on URL {}", httpServletRequest.getRequestURI());
        TrackingInputStream trackingInputStream = trackingInputStreamFactory.createForFileUpload(httpServletRequest.getInputStream());

        long readStartTime = System.nanoTime();
        byte[] readBuff = new byte[2000000];
        while (trackingInputStream.read(readBuff) != 0) ;
        long readEndTime = System.nanoTime();

        trackingInputStream.close();

        long readTimeNano = readEndTime - readStartTime;

        ServletHelper.setJsonResponse(httpServletResponse, Map.of(
                "result", "success",
                "countOfReadBytes", trackingInputStream.getCountOfReadBytes(),
                "countOfReadKBytes", trackingInputStream.getCountOfReadBytes() / (double) (1000),
                "countOfReadMBytes", trackingInputStream.getCountOfReadBytes() / (double) (1000 * 1000),
                "readTimeNanos", readTimeNano,
                "readTimeMillis", readTimeNano / (double) (1000 * 1000),
                "readTimeSeconds", readTimeNano / (double) (1000 * 1000 * 1000)
        ), 200);
    }

    @Value("${streambenchmarkfilter.enable:false}")
    public void setEnableStreamBenchmarkFilter(Boolean enableStreamBenchmarkFilter) {
        this.enableStreamBenchmarkFilter = enableStreamBenchmarkFilter;
    }
}