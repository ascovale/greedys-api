package com.application.restaurant.persistence.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "closed_day")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClosedDay {
	@Id		
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	LocalDate closedDate;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "restaurant_id")
	Restaurant restaurant;
	
}
