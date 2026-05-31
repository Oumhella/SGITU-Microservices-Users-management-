package com.g7suivivehicules.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement Kafka {@code vehicle.registered} consommé par G4 (coordination).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class G7VehicleRegisteredEventDTO {

    private String vehiculeId;
    private String immatriculation;
    private String type;
    private String statut;
    private String timestamp;
}
