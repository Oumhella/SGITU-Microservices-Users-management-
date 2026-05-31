package com.sgitu.g4.dto;

import lombok.Data;

/**
 * Payload Kafka {@code vehicle.registered} publié par G7 à la création d'un véhicule.
 */
@Data
public class G7VehicleRegisteredMessage {

	private String vehiculeId;
	private String immatriculation;
	private String type;
	private String statut;
	private String timestamp;
}
