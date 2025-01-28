package com.application.persistence.model.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantUser;
import com.application.persistence.model.user.User;

@Entity
@Table(name = "reservation")
public class Reservation {
	@Id		
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_restaurant")
	private Restaurant restaurant;
	@DateTimeFormat(pattern = "yyyy/MM/dd")
	@Temporal(TemporalType.DATE)
	@Column(name = "r_date")
	private LocalDate date;
	@Column(name = "creation_date")
	private LocalDate creationDate;
	private ClientInfo user_info;
	@ManyToOne(optional = true)
	@JoinColumn(name = "user_id")
	private User user;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idslot")
	private	Slot slot;
	private Integer pax;
	private Integer kids = 0;
	private String notes;
	private Boolean rejected = false;
	private Boolean accepted = false;
	private Boolean seated = false;
	private Boolean noShow = false;
	private Boolean cancelled = false;
	Integer version=1;
    // si potrebbe fare una tabella a parte per i log
	@DateTimeFormat(pattern = "yyyy/MM/dd/HH:mm")
	private LocalDateTime lastModificationTime;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idrestaurant_user")
 	private RestaurantUser restaurantUser;

	@ManyToOne(fetch = FetchType.LAZY)
	private com.application.persistence.model.restaurant.Table table;

	public com.application.persistence.model.restaurant.Table getTable() {
		return table;
	}

	public void setTable(com.application.persistence.model.restaurant.Table table) {
		this.table = table;
	}


	public ClientInfo get_user_info() {
		return user_info;
	}

	public void set_user_info(ClientInfo user_info) {
		this.user_info = user_info;
	}

	public Boolean getSeated() {
		return seated;
	}

	public void setSeated(Boolean seated) {
		this.seated = seated;
	}

	public Long get_user_id() {
		if (user == null) {
			throw new IllegalArgumentException("User id is null, the reservation is anonymous.");
		}
		return user.getId();
	}

	public LocalDate getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDate creationDate) {
		this.creationDate = creationDate;
	}

	public Long getId() {
		return id;
	}

	public Integer getPax() {
		return pax;
	}

	public void setPax(Integer pax) {
		this.pax = pax;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public Slot getSlot() {
		return slot;
	}

	public void setSlot(Slot slot) {
		this.slot = slot;
	}

	public Integer getKids() {
		return kids;
	}

	public void setKids(Integer kids) {
		this.kids = kids;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Boolean getRejected() {
		return rejected;
	}

	public void setRejected(Boolean rejected) {
		this.rejected = rejected;
	}

	public Boolean getAccepted() {
		return accepted;
	}

	public void setAccepted(Boolean accepted) {
		this.accepted = accepted;
	}

	public Boolean getNoShow() {
		return noShow;
	}

	public void setNoShow(Boolean noShow) {
		this.noShow = noShow;
	}

	public LocalDateTime getLastModificationTime() {
		return lastModificationTime;
	}

	public void setLastModificationTime(LocalDateTime lastModificationTime) {
		this.lastModificationTime = lastModificationTime;
	}

	public RestaurantUser getRestaurantUser() {
		return restaurantUser;
	}

	public void setRestaurantUser(RestaurantUser restaurantUser) {
		this.restaurantUser = restaurantUser;
	}

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;

	}

	public Restaurant getRestaurant() {
		return restaurant;
	}
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Boolean getCancelled() {
		return cancelled;
	}

	public void setCancelled(Boolean cancelled) {
		this.cancelled = cancelled;
	}

	public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
	public void setId(Long id) {
		this.id = id;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public LocalDateTime getReservationDateTime() {
        if (date != null && slot != null && slot.getStart() != null) {
            return LocalDateTime.of(date, slot.getStart());
        }
        return null;
    }


	public boolean isAfterNoShowTimeLimit(LocalDateTime dateTime) {
		long noShowTimeLimit = this.getRestaurant().getNoShowTimeLimit();
		LocalDateTime reservationDateTime = getReservationDateTime();
		if (reservationDateTime == null) {
			throw new IllegalArgumentException("Reservation date time or slot start time is null.");
		}
		LocalDateTime noShowDeadline = reservationDateTime.plusMinutes(noShowTimeLimit);
		return dateTime.isAfter(noShowDeadline);
	}

}
