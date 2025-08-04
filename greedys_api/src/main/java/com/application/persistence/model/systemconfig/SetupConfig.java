package com.application.persistence.model.systemconfig;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupConfig {
    @Id
    private Long id;
    @Builder.Default
    private boolean alreadySetup=false;
    @Builder.Default
    private boolean dataUploaded=false;

}