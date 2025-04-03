package com.application.persistence.model.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.persistence.model.admin.Admin;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantUser;

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
	@ManyToOne(optional = true)
	@JoinColumn(name = "customer_id")
	private Customer customer;
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
	private Boolean deleted = false;
	Integer version=1;
	@DateTimeFormat(pattern = "yyyy/MM/dd/HH:mm")
	private LocalDateTime lastModificationTime;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idrestaurant_user")
 	private RestaurantUser restaurantUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creator_customer_id", nullable = true)
	private Customer creatorCustomer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creator_restaurant_user_id", nullable = true)
	private RestaurantUser creatorRestaurantUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creator_admin_id", nullable = true)
	private Admin creatorAdmin;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "last_modifier_customer_id", nullable = true)
	private Customer lastModifierCustomer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "last_modifier_restaurant_user_id", nullable = true)
	private RestaurantUser lastModifierRestaurantUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "last_modifier_admin_id", nullable = true)
	private Admin lastModifierAdmin;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "acceptor_restaurant_user_id", nullable = true)
	private RestaurantUser acceptorRestaurantUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "acceptor_admin_id", nullable = true)
	private Admin acceptorAdmin;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_customer_id", nullable = true)
	private Customer createdByCustomer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_restaurant_user_id", nullable = true)
	private RestaurantUser createdByRestaurantUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_admin_id", nullable = true)
	private Admin createdByAdmin;

	public Object getCreatedBy() {
		if (createdByCustomer != null) {
			return createdByCustomer;
		} else if (createdByRestaurantUser != null) {
			return createdByRestaurantUser;
		} else if (createdByAdmin != null) {
			return createdByAdmin;
		}
		return null;
	}

	// Separate setters for createdBy
	public void setCreatedByCustomer(Customer customer) {
		this.createdByCustomer = customer;
		this.createdByRestaurantUser = null;
		this.createdByAdmin = null;
	}

	public void setCreatedByRestaurantUser(RestaurantUser restaurantUser) {
		this.createdByRestaurantUser = restaurantUser;
		this.createdByCustomer = null;
		this.createdByAdmin = null;
	}

	public void setCreatedByAdmin(Admin admin) {
		this.createdByAdmin = admin;
		this.createdByCustomer = null;
		this.createdByRestaurantUser = null;
	}

	// Separate setters for lastModifier
	public void setLastModifiedByCustomer(Customer customer) {
		this.lastModifierCustomer = customer;
		this.lastModifierRestaurantUser = null;
		this.lastModifierAdmin = null;
	}

	public void setLastModifiedByRestaurantUser(RestaurantUser restaurantUser) {
		this.lastModifierRestaurantUser = restaurantUser;
		this.lastModifierCustomer = null;
		this.lastModifierAdmin = null;
	}

	public void setLastModifiedByAdmin(Admin admin) {
		this.lastModifierAdmin = admin;
		this.lastModifierCustomer = null;
		this.lastModifierRestaurantUser = null;
	}

	// Separate setters for acceptor
	public void setAcceptedByRestaurantUser(RestaurantUser restaurantUser) {
		this.acceptorRestaurantUser = restaurantUser;
		this.acceptorAdmin = null;
	}

	public void setAcceptedByAdmin(Admin admin) {
		this.acceptorAdmin = admin;
		this.acceptorRestaurantUser = null;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	private com.application.persistence.model.restaurant.Table table;

	public com.application.persistence.model.restaurant.Table getTable() {
		return table;
	}

	public void setTable(com.application.persistence.model.restaurant.Table table) {
		this.table = table;
	}

	public Boolean getSeated() {
		return seated;
	}

	public void setSeated(Boolean seated) {
		this.seated = seated;
	}

	public Long getCustomerId() {
		if (customer == null) {
			throw new IllegalArgumentException("User id is null, the reservation is anonymous.");
		}
		return customer.getId();
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
	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
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

	public Customer getCreatorCustomer() {
		return creatorCustomer;
	}

	public void setCreatorCustomer(Customer creatorCustomer) {
		this.creatorCustomer = creatorCustomer;
	}

	public RestaurantUser getCreatorRestaurantUser() {
		return creatorRestaurantUser;
	}

	public void setCreatorRestaurantUser(RestaurantUser creatorRestaurantUser) {
		this.creatorRestaurantUser = creatorRestaurantUser;
	}

	public Admin getCreatorAdmin() {
		return creatorAdmin;
	}

	public void setCreatorAdmin(Admin creatorAdmin) {
		this.creatorAdmin = creatorAdmin;
	}

	public Customer getLastModifierCustomer() {
		return lastModifierCustomer;
	}

	public void setLastModifierCustomer(Customer lastModifierCustomer) {
		this.lastModifierCustomer = lastModifierCustomer;
	}

	public RestaurantUser getLastModifierRestaurantUser() {
		return lastModifierRestaurantUser;
	}

	public void setLastModifierRestaurantUser(RestaurantUser lastModifierRestaurantUser) {
		this.lastModifierRestaurantUser = lastModifierRestaurantUser;
	}

	public Admin getLastModifierAdmin() {
		return lastModifierAdmin;
	}

	public void setLastModifierAdmin(Admin lastModifierAdmin) {
		this.lastModifierAdmin = lastModifierAdmin;
	}

	public RestaurantUser getAcceptorRestaurantUser() {
		return acceptorRestaurantUser;
	}

	public void setAcceptorRestaurantUser(RestaurantUser acceptorRestaurantUser) {
		this.acceptorRestaurantUser = acceptorRestaurantUser;
	}

	public Admin getAcceptorAdmin() {
		return acceptorAdmin;
	}

	public void setAcceptorAdmin(Admin acceptorAdmin) {
		this.acceptorAdmin = acceptorAdmin;
	}
}
