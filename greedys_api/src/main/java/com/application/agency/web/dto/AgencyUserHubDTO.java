package com.application.agency.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyUserHubDTO {
    private Long id;
    private String username;  // email
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String status;
}
