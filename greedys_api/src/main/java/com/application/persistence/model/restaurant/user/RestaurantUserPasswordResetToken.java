package com.application.persistence.model.restaurant.user;


import java.time.LocalDateTime;

import com.application.persistence.model.customer.Customer;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class RestaurantUserPasswordResetToken {

    private static final int EXPIRATION = 60 * 24;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String token;
    @OneToOne(targetEntity = Customer.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private RestaurantUser restaurantUser;
    private LocalDateTime expiryDate;

	public RestaurantUserPasswordResetToken() {
        super();
    }

    public RestaurantUserPasswordResetToken(final String token) {
        super();
        this.token = token;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    public RestaurantUserPasswordResetToken(final String token, final RestaurantUser restaurantUser) {
        super();
        this.token = token;
        this.restaurantUser = restaurantUser;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public RestaurantUser getRestaurantUser() {
        return restaurantUser;
    }

    public void setRestaurantUser(final RestaurantUser restaurantUser) {
        this.restaurantUser = restaurantUser;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(final LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
    private LocalDateTime calculateExpiryDate(final int expiryTimeInMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDateTime = now.plusMinutes(expiryTimeInMinutes);
        return expiryDateTime;
    }

    public void updateToken(final String token) {
        this.token = token;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    public static int getExpiration() {
		return EXPIRATION;
	}

	public void setId(Long id) {
		this.id = id;
	}




	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((expiryDate == null) ? 0 : expiryDate.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        result = prime * result + ((restaurantUser == null) ? 0 : restaurantUser.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RestaurantUserPasswordResetToken other = (RestaurantUserPasswordResetToken) obj;
        if (expiryDate == null) {
            if (other.expiryDate != null) {
                return false;
            }
        } else if (!expiryDate.equals(other.expiryDate)) {
            return false;
        }
        if (token == null) {
            if (other.token != null) {
                return false;
            }
        } else if (!token.equals(other.token)) {
            return false;
        }
        if (restaurantUser == null) {
            if (other.restaurantUser != null) {
                return false;
            }
        } else if (!restaurantUser.equals(other.restaurantUser)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Token [String=").append(token).append("]").append("[Expires").append(expiryDate).append("]");
        return builder.toString();
    }

}
