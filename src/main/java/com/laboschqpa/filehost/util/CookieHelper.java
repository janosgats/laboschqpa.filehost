package com.laboschqpa.filehost.util;

import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Base64;

public class CookieHelper {
    private static final Base64.Decoder base64Decoder = Base64.getDecoder();

    public static String extractUnencodedSessionCookieOrThrow(HttpServletRequest httpServletRequest) {
        final Cookie encodedSessionCookie = extractSessionCookieOrThrow(httpServletRequest);
        return new String(base64Decoder.decode(encodedSessionCookie.getValue()));
    }

    public static Cookie extractSessionCookieOrThrow(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SESSION".equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        throw new InvalidHttpRequestException("Cannot found session cookie! You are probably not logged in.");
    }
}
