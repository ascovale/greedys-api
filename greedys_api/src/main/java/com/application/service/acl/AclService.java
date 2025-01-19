package com.application.service.acl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;

import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantUser;
import com.application.persistence.model.user.User;

import jakarta.transaction.Transactional;

public class AclService {
	@Autowired
	private MutableAclService mutableAclService;

    @Transactional
    public void setReservationAcl(User user, Reservation reservation, Restaurant restaurant) {
		// Imposta i permessi ACL per l'oggetto Reservation
		ObjectIdentity oi = new ObjectIdentityImpl(Reservation.class, reservation.getId());
		MutableAcl acl = mutableAclService.createAcl(oi);
		Sid sid = new PrincipalSid(user.getUsername());
		acl.insertAce(acl.getEntries().size(), BasePermission.READ, sid, true);
		acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, sid, true);
		mutableAclService.updateAcl(acl);

        // Imposta i permessi ACL per ogni userRestaurant del restaurant
        for (RestaurantUser userRestaurant : restaurant.getRestaurantUsers()) {
            Sid restaurantUserSid = new PrincipalSid(userRestaurant.getUser().getUsername());
            acl.insertAce(acl.getEntries().size(), BasePermission.READ, restaurantUserSid, true);
            acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, restaurantUserSid, true);
            acl.insertAce(acl.getEntries().size(), BasePermission.DELETE, restaurantUserSid, true);
        }

    }

    /*    @PreAuthorize("@aclService.hasPermission(#reservationId, T(org.springframework.security.acls.domain.BasePermission).WRITE)")
 

    public boolean hasPermission(Long reservationId, Permission permission) {
        User currentUser = getCurrentUser();
        Sid sid = new PrincipalSid(currentUser.getUsername());
        ObjectIdentity oi = new ObjectIdentityImpl(Reservation.class, reservationId);
        Acl acl = aclService.readAclById(oi);
        return acl.isGranted(List.of(permission), List.of(sid), false);
    } */
}
