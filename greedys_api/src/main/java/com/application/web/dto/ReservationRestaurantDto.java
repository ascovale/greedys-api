package com.application.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReservationRestaurantDto", description = "DTO for restaurant reservation details")
public class ReservationRestaurantDto {

	private Long idSlot;
	private Integer pax;
	private Integer kids=0;
	private String name;
	private String surname;
	private String numero_di_telefono;
	private String email;
	private String notes;
	public ReservationRestaurantDto() {
	}
	public Long getIdSlot() {
		return idSlot;
	}
	public void setIdSlot(Long idSlot) {
		this.idSlot = idSlot;
	}
	public Integer getPax() {
		return pax;
	}
	public void setPax(Integer pax) {
		this.pax = pax;
	}
	public Integer getKids() {
		return kids;
	}
	public void setKids(Integer kids) {
		this.kids = kids;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSurname() {
		return surname;
	}
	public void setSurname(String surname) {
		this.surname = surname;
	}
	public String getNumero_di_telefono() {
		return numero_di_telefono;
	}
	public void setNumero_di_telefono(String numero_di_telefono) {
		this.numero_di_telefono = numero_di_telefono;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}	
	
}
