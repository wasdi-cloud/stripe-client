package com.wasdi.stripeclient.model;

import java.util.List;

import lombok.Data;

@Data
public class StripeConfigEntry {

	private String apiKey;
	private List<ProductConfigEntry> products;

}
