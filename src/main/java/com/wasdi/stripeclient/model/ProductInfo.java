package com.wasdi.stripeclient.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ProductInfo {

	private String id;
	private String name;
	private String description;
	private String type;
	private Long amount;
	private String currency;
	private List<String> images = new ArrayList<>();

}
