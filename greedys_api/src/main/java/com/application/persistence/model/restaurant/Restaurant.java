package com.application.persistence.model.restaurant;

import java.util.Collection;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.application.persistence.model.Image;
import com.application.persistence.model.reservation.Service;

/*import com.application.persistence.model.CompanyReservation;
@NamedNativeQuery(name = "Restaurant.findBySearchTermNamedNative",
query="SELECT * FROM Eestaurant r WHERE " +
        "LOWER(r.name) LIKE LOWER(CONCAT('%',:searchTerm, '%'))" +
        "ORDER BY t.name ASC",
resultClass = Restaurant.class
)*/
@NamedQuery(name = "Restaurant.findBySearchTermNamed", query = "SELECT t FROM Restaurant t WHERE "
		+ "LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))" + "ORDER BY t.name ASC")
@Entity
@Table(name = "restaurant")
public class Restaurant {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	@Column(name = "date_creation")
	private LocalDate creationDate;
	private String email;
	@Column(name = "telephone")
	private String tel;
	private String address;
	@Column(name = "post_code")
	private String postCode;
	@Column(name = "PI")
	private String pI;
	@Column(name = "description")
	private String description;
	// TODO cambiare il fetch in LAZY
	// bisogna fare in modo che la pageable prende anche le foto basta fare il join
	// fetch ma è solo in jpql
	// quindi da vedere come fare con una query normale o come scrivere in jpql
	// si potrebbe fare un JOIN FETCH o fetch join con RestaurantImage ir nella
	// clausola from
	@OneToMany(fetch = FetchType.EAGER, mappedBy = "restaurant")
	private List<Service> services;
	@OneToMany(mappedBy = "restaurant")
	private Collection<RestaurantUser> restaurantUsers;
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "restaurant")
	//@JsonManagedReference
    private List<Image> restaurantImages;
	
	
	public Collection<RestaurantUser> getRestaurantUsers() {
		return restaurantUsers;
	}

	public void setRestaurantUsers(Collection<RestaurantUser> restaurantUsers) {
		this.restaurantUsers = restaurantUsers;
	}

	public List<Image> getRestaurantImages() {
		return restaurantImages;
	}

	public void setRestaurantImages(List<Image> restaurantImages) {
		this.restaurantImages = restaurantImages;
	}

	public void setServices(List<Service> services) {
		this.services = services;
	}

	public List<Service> getServices() {
		return services;
	}

	public void setRestaurantServizi(List<Service> services) {
		this.services = services;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/*
	 * @OneToMany(fetch = FetchType.LAZY, mappedBy = "restaurant") private Set<Menu>
	 * menu;
	 * 
	 * public Set<Menu> getMenu() { return menu; }
	 * 
	 * public void setMenu(Set<Menu> menu) { this.menu = menu; }
	 */

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Restaurant [id=" + id + "]";
	}

	public Restaurant() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LocalDate getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDate creationDate) {
		this.creationDate = creationDate;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTel() {
		return tel;
	}

	public void setTel(String tel) {
		this.tel = tel;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPostCode() {
		return postCode;
	}

	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}

	public String getpI() {
		return pI;
	}

	public void setpI(String pI) {
		this.pI = pI;
	}

}
