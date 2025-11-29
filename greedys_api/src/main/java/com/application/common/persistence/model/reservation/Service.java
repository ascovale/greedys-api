package com.application.common.persistence.model.reservation;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.menu.Menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;

	@ManyToOne(fetch = FetchType.EAGER) // qui potrebbe essere utile condividere il servizio tra più ristoranti in
										// alcuni casi
	@JoinColumn(name = "restaurant_id")
	private Restaurant restaurant;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "service_service_type", joinColumns = @JoinColumn(name = "service_id"), inverseJoinColumns = @JoinColumn(name = "type_id"))
	@Builder.Default
	private Set<ServiceType> serviceTypes = new HashSet<>();

	@OneToMany(mappedBy = "service", fetch = FetchType.EAGER)
	private Set<Slot> slots;

	@Column(name = "info")
	private String info;

	@Builder.Default
	private Boolean deleted = false;


	private LocalDate validFrom;
	private LocalDate validTo;

	@Builder.Default
	private boolean active = true;

	@Builder.Default
	private boolean enabled = true;

	// colore del servizio
	private String color;
	
	@ManyToMany
	@JoinTable(
		name = "menu_service",
		joinColumns = @JoinColumn(name = "service_id"),
		inverseJoinColumns = @JoinColumn(name = "menu_id")
	)
	private List<Menu> menus;

	public boolean isActiveNow() {
		LocalDate today = LocalDate.now();
		return isActiveInDate(today);
	}

	public boolean isActiveInDate(LocalDate date) {
		if (validFrom != null && validTo != null) {
			if (date.isBefore(validFrom) || date.isAfter(validTo)) {
				return false;
			}
		}
		return active;
	}

	@OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
	@Builder.Default
	private Set<Reservation> reservations = new HashSet<>();

	public void addServiceType(ServiceType pranzoType) {
		if (serviceTypes == null) {
			serviceTypes = new HashSet<>();
		}
		serviceTypes.add(pranzoType);
    }

}
