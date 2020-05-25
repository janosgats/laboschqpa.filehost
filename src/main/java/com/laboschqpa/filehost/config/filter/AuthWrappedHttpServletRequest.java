package com.laboschqpa.filehost.config.filter;

import com.laboschqpa.filehost.exceptions.AuthInterServiceCallRequiredException;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@Getter
public class AuthWrappedHttpServletRequest extends HttpServletRequestWrapper {
    private AuthMethod authMethod;
    private WrappedFileServingRequestDto wrappedFileServingRequestDto;

    public AuthWrappedHttpServletRequest(HttpServletRequest request, AuthMethod authMethod, WrappedFileServingRequestDto wrappedFileServingRequestDto) {
        super(request);
        this.authMethod = authMethod;
        this.wrappedFileServingRequestDto = wrappedFileServingRequestDto;
    }

    public void assertIsAuthInterServiceCall() {
        if (authMethod != AuthMethod.AUTH_INTER_SERVICE) {
            throw new AuthInterServiceCallRequiredException("The request wasn't authed by AuthInterService!");
        }
    }

}
