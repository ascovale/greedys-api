package com.application.web.dto.get;

import com.application.persistence.model.restaurant.Table;

public class TableDTO {
    
        private Long id;
        private String name;
    
        public TableDTO(Table table) {
            this.id = table.getId();
            this.name = table.getName();
        }
    
        public Long getId() {
            return id;
        }
    
        public String getName() {
            return name;
        }
    
}
