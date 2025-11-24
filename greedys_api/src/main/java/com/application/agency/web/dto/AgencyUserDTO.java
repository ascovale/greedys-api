package com.application.agency.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyUserDTO {
    private Long id;
    private String username;  // email
    private String email;
    private String name;      // firstName
    private String surname;   // lastName
    private String phoneNumber;
    private Long agencyId;
    private String status;
}
