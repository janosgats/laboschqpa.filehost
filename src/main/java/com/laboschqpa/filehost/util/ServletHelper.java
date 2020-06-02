package com.laboschqpa.filehost.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ServletHelper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void setJsonResponse(HttpServletResponse servletResponse, Object bodyObject, int httpStatus) {
        servletResponse.setStatus(httpStatus);
        servletResponse.setContentType("application/json");
        servletResponse.setCharacterEncoding("UTF-8");

        String body;
        try {
            body = objectMapper.writeValueAsString(bodyObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try {
            PrintWriter out = servletResponse.getWriter();
            out.print(body);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
