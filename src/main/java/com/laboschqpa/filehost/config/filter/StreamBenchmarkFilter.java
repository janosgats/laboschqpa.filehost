package com.laboschqpa.filehost.config.filter;

import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStreamFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

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

        if (enableStreamBenchmarkFilter && httpServletRequest.getRequestURI().contains("/benchmark/streamspeed")) {
            logger.warn("Doing stream benchmark on URL {}", httpServletRequest.getRequestURI());
            TrackingInputStream trackingInputStream = trackingInputStreamFactory.createForFileUpload(httpServletRequest.getInputStream());

            byte[] readBuff = new byte[2000000];
            while (trackingInputStream.read(readBuff) != 0);

            trackingInputStream.close();
        } else {
            chain.doFilter(request, response);
        }
    }

    @Value("${streambenchmarkfilter.enable:false}")
    public void setEnableStreamBenchmarkFilter(Boolean enableStreamBenchmarkFilter) {
        this.enableStreamBenchmarkFilter = enableStreamBenchmarkFilter;
    }
}