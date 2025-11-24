package com.application.agency.web.dto;

import com.application.common.web.dto.security.UserAuthResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyUserHubDTO extends UserAuthResponse {
    private Long id;
    private String username;  // email
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String status;
    
    @Override
    public String getUserType() {
        return "AGENCY_HUB";
    }
}
