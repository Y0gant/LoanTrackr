package com.loantrackr.dto.response;

import com.loantrackr.enums.AuthProvider;
import com.loantrackr.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private AuthProvider provider;
    private boolean isActive;
    private boolean isEmailVerified;


}