package com.application.common.web.dto.get;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "CustomerStatisticsDTO", description = "DTO for customer statistics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatisticsDTO {
    
    @Schema(description = "Total number of reservations made by the customer", example = "25")
    private long totalReservations;
    
    @Schema(description = "Number of accepted reservations", example = "20")
    private long acceptedReservations;
    
    @Schema(description = "Number of pending reservations", example = "3")
    private long pendingReservations;
    
    @Schema(description = "Number of rejected reservations", example = "2")
    private long rejectedReservations;
    
    @Schema(description = "Number of no-show reservations", example = "1")
    private long noShowReservations;
    
    @Schema(description = "Number of seated reservations", example = "18")
    private long seatedReservations;
    
    @Schema(description = "Number of deleted reservations", example = "1")
    private long deletedReservations;
    
    @Schema(description = "No-show rate as a percentage", example = "5.0")
    private double noShowRate;
    
    @Schema(description = "Acceptance rate as a percentage", example = "80.0")
    private double acceptanceRate;
    
    @Schema(description = "Completion rate (seated/accepted) as a percentage", example = "90.0")
    private double completionRate;

    public CustomerStatisticsDTO(long totalReservations, long acceptedReservations, 
                                long pendingReservations, long rejectedReservations, 
                                long noShowReservations, long seatedReservations, 
                                long deletedReservations) {
        this.totalReservations = totalReservations;
        this.acceptedReservations = acceptedReservations;
        this.pendingReservations = pendingReservations;
        this.rejectedReservations = rejectedReservations;
        this.noShowReservations = noShowReservations;
        this.seatedReservations = seatedReservations;
        this.deletedReservations = deletedReservations;
        
        // Calculate rates
        if (totalReservations > 0) {
            this.noShowRate = (double) noShowReservations / totalReservations * 100;
            this.acceptanceRate = (double) acceptedReservations / totalReservations * 100;
        } else {
            this.noShowRate = 0.0;
            this.acceptanceRate = 0.0;
        }
        
        if (acceptedReservations > 0) {
            this.completionRate = (double) seatedReservations / acceptedReservations * 100;
        } else {
            this.completionRate = 0.0;
        }
    }
}
