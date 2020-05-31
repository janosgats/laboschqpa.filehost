package com.laboschqpa.filehost.api.dto;

import com.laboschqpa.filehost.enums.FileAccessType;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import lombok.*;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ExternalIndexedFileServingRequestDto {
    private HttpMethod httpMethod;
    private String csrfToken;
    private Long indexedFileId;
    private FileAccessType fileAccessType;

    public static class Factory {
        public static ExternalIndexedFileServingRequestDto createFrom(HttpServletRequest httpServletRequest) {
            HttpMethod httpMethod = HttpMethod.resolve(httpServletRequest.getMethod());
            if (httpMethod == null)
                throw new InvalidHttpRequestException("No valid HttpMethod is specified! (" + httpServletRequest.getMethod() + ")");
            String csrfToken = httpServletRequest.getHeader("X-CSRF-TOKEN");

            FileAccessType fileAccessType = getRequestedFileAccessType(httpServletRequest);

            Long requestedIndexedFileId = null;
            if (fileAccessType == FileAccessType.READ || fileAccessType == FileAccessType.DELETE)
                requestedIndexedFileId = getRequestedIndexedFileId(httpServletRequest);

            return ExternalIndexedFileServingRequestDto
                    .builder()
                    .httpMethod(httpMethod)
                    .csrfToken(csrfToken)
                    .indexedFileId(requestedIndexedFileId)
                    .fileAccessType(fileAccessType)
                    .build();
        }

        private static FileAccessType getRequestedFileAccessType(HttpServletRequest httpServletRequest) {
            HttpMethod httpMethod = HttpMethod.resolve(httpServletRequest.getMethod());
            if (httpMethod == null)
                throw new InvalidHttpRequestException("No valid HttpMethod is specified! (" + httpServletRequest.getMethod() + ")");

            switch (httpMethod) {
                case GET:
                    return FileAccessType.READ;
                case POST:
                    return FileAccessType.CREATE_NEW;
                case DELETE:
                case PATCH:
                    throw new InvalidHttpRequestException("Only READ and CREATE_NEW requests should be formed into ExternalIndexedFileServingRequestDto.");
                default:
                    throw new InvalidHttpRequestException("Cannot map given HttpMethod to FileAccessType: " + httpMethod);
            }
        }

        private static Long getRequestedIndexedFileId(HttpServletRequest httpServletRequest) {
            HashSet<String> indexedFileIdParamValues = getHashSetOfParamValues(httpServletRequest, "id", "indexedFileId");

            if (indexedFileIdParamValues.size() > 1)
                throw new InvalidHttpRequestException("indexedFileId is specified multiple times with different values.");
            else if (indexedFileIdParamValues.size() == 0)
                throw new InvalidHttpRequestException("No indexedFileId parameter is present.");

            return Long.parseLong(indexedFileIdParamValues.iterator().next());
        }

        private static HashSet<String> getHashSetOfParamValues(HttpServletRequest httpServletRequest, String... paramAliases) {
            final HashSet<String> values = new HashSet<>();
            for (String name : paramAliases) {
                final String[] buffParams = httpServletRequest.getParameterValues(name);
                if (buffParams != null)
                    values.addAll(Arrays.asList(buffParams));
            }
            return values;
        }
    }
}
