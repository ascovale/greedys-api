package com.application.persistence.model.menu;

import java.util.List;

import com.application.persistence.model.reservation.Service;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description; 
    @Builder.Default
    private boolean enabled = true;
    @ManyToMany
    @JoinTable(
        name = "menu_service",
        joinColumns = @JoinColumn(name = "menu_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<Service> services;

}
