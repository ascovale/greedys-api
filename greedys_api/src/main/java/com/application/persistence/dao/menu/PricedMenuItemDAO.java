package com.application.persistence.dao.menu;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.application.persistence.model.menu.MenuHasItem;
import com.application.persistence.model.menu.MenuHasItemId;
import com.application.web.dto.get.PricedMenuItemDTO;

public interface PricedMenuItemDAO extends JpaRepository<MenuHasItem, MenuHasItemId> {

    @Query("""
            SELECT new com.application.web.dto.get.PricedMenuItemDTO(i, mhi.price) 
            FROM MenuItem i
            JOIN i.menusWithItem mhi
            WHERE mhi.menu = :id
            """)
    Collection<PricedMenuItemDTO> findPricedItemsByMenuId(Long id);

}
