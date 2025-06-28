package com.application.web.dto.post.admin;

import com.application.web.dto.post.NewBaseReservationDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminNewReservationDTO", description = "DTO for creating a new admin reservation")
public class AdminNewReservationDTO extends NewBaseReservationDTO {
    private Long restaurant_id;
    private Long user_id;
    private Boolean accept;
    private Boolean seated;
    private Boolean noShow;
    private Boolean rejected;

    public Boolean getRejected() {
        return rejected;
    }
    public void setRejected(Boolean rejected) {
        this.rejected = rejected;
    }
    public Boolean getAccept() {
        return accept;
    }
    public void setAccept(Boolean accept) {
        this.accept = accept;
    }
    public Boolean getSeated() {
        return seated;
    }
    public void setSeated(Boolean seated) {
        this.seated = seated;
    }
    public Boolean getNoShow() {
        return noShow;
    }
    public void setNoShow(Boolean noShow) {
        this.noShow = noShow;
    }
    public Boolean isAnonymous() {
        return user_id == null;
    }
    public Long getUser_id() {
        if (user_id == null) {
            throw new IllegalArgumentException("User id is null, the reservation is anonymous.");
        }
        return user_id;
    }
    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }
    public Long getRestaurant_id() {
        return restaurant_id;
    }
    public void setRestaurant_id(Long restaurant_id) {
        this.restaurant_id = restaurant_id;
    }
}

