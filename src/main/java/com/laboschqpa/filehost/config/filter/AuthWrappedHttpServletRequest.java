package com.laboschqpa.filehost.config.filter;

import com.laboschqpa.filehost.exceptions.AuthInterServiceCallRequiredException;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@Getter
public class AuthWrappedHttpServletRequest extends HttpServletRequestWrapper {
    private AuthMethod authMethod;
    private WrappedExternalFileServingRequestDto wrappedExternalFileServingRequestDto;

    public AuthWrappedHttpServletRequest(HttpServletRequest request, AuthMethod authMethod, WrappedExternalFileServingRequestDto wrappedExternalFileServingRequestDto) {
        super(request);
        this.authMethod = authMethod;
        this.wrappedExternalFileServingRequestDto = wrappedExternalFileServingRequestDto;
    }

    public void assertIsAuthInterServiceCall() {
        if (authMethod != AuthMethod.AUTH_INTER_SERVICE) {
            throw new AuthInterServiceCallRequiredException("The request wasn't authed by AuthInterService!");
        }
    }

}
