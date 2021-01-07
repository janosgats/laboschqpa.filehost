package com.laboschqpa.filehost.service.fileservingauth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthorizeRequestResult {
    private Long loggedInUserId;
    private Long loggedInUserTeamId;
}