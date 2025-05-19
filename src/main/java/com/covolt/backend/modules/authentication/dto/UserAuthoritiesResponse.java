package com.covolt.backend.modules.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthoritiesResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private List<String> roles;
    private List<String> permissions;
}
