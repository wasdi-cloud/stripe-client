package com.wasdi.stripeclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentLink;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.wasdi.stripeclient.model.ProductConfigEntry;
import com.wasdi.stripeclient.model.ProductInfo;
import com.wasdi.stripeclient.model.StripeConfigEntry;
import com.wasdi.stripeclient.service.StripeService;

@SpringBootApplication
public class StripeClientApplication implements CommandLineRunner {

	private static Logger LOGGER = LoggerFactory.getLogger(StripeClientApplication.class);

	@Value("${stripe.api.key}")
	private String stripeApiKey;

	@Value("#{${wasdi.standard.product.metadata}}")
	private Map<String, String> metadataStandard;

	@Value("#{${wasdi.professional.product.metadata}}")
	private Map<String, String> metadataProfessional;


	@Autowired
	private StripeService stripeService;

	public static void main(String[] args) {
		SpringApplication.run(StripeClientApplication.class, args);
	}

	@Override
	public void run(String... args) throws StripeException {
		LOGGER.info("start running the Stripe client");

		List<ProductInfo> products = readProductInformation();

		List<ProductConfigEntry> productConfigEntries = populateStripeAccount(products);

		StripeConfigEntry stripeConfigEntry = new StripeConfigEntry();

		stripeConfigEntry.setApiKey(stripeApiKey);
		stripeConfigEntry.setProducts(productConfigEntries);

		writeProductConfigEntriesToFile(stripeConfigEntry);

		LOGGER.info("end running the Stripe client");
	}

	private List<ProductConfigEntry> populateStripeAccount(List<ProductInfo> products) throws StripeException {
		LOGGER.info("start populating the Stripe account");

		List<ProductConfigEntry> productConfigEntries = new ArrayList<>();

		for (ProductInfo productInfo : products) {
			ProductConfigEntry productConfigEntry = createProduct(productInfo);

			if (productConfigEntry == null) {
				continue;
			}

			productConfigEntries.add(productConfigEntry);
		}

		LOGGER.info("end populating the Stripe account");

		return productConfigEntries;
	}

	private ProductConfigEntry createProduct(ProductInfo productInfo) throws StripeException {
		LOGGER.info("createProduct: " + productInfo.toString());

		try {
			Map<String,String> metadata = productInfo.getType().equalsIgnoreCase("professional") ? metadataProfessional : metadataStandard;

			Product product = stripeService.createProduct(productInfo.getId(), productInfo.getName(), productInfo.getDescription(), metadata, productInfo.getImages());

			Price price = stripeService.createPrice(product.getId(), productInfo.getCurrency(), productInfo.getAmount());

			PaymentLink paymentLink = stripeService.createPaymentLink(price.getId());


			ProductConfigEntry productConfigEntry = new ProductConfigEntry();
			productConfigEntry.setId(productInfo.getId());
			productConfigEntry.setUrl(paymentLink.getUrl());

			return productConfigEntry;
		} catch(InvalidRequestException e) {
			LOGGER.error("Error: " + e.getMessage());
			return null;
		}
	}

	private List<ProductInfo> readProductInformation() {
		JsonFactory jsonFactory = new JsonFactory();
		jsonFactory.enable(JsonParser.Feature.ALLOW_COMMENTS);
		ObjectMapper mapper = new ObjectMapper(jsonFactory);

		TypeReference<List<ProductInfo>> typeReference = new TypeReference<List<ProductInfo>>() {};

		String filePath = "products.json";
		
		try {
			File file = new File(filePath);

			InputStream inputStream = null;

			if (file.exists()) {
				LOGGER.info(filePath + " file exists on the same folder. using this external file.");
				inputStream = new FileInputStream(file);
			} else {
				LOGGER.info(filePath + " file does not exist on the same folder. using the internal provided file.");
			}

			if (inputStream == null) {
				inputStream = StripeClientApplication.class.getResourceAsStream("/" + filePath);

				if (inputStream == null) {
					LOGGER.info(filePath + " internal file cannot be read.");
					System.exit(0);
				} else {
					LOGGER.info(filePath + " internal file read successfully.");
				}
			} else {
				LOGGER.info(filePath + " external file read successfully.");
			}

			List<ProductInfo> products = mapper.readValue(inputStream, typeReference);
			LOGGER.info(filePath + " file contains " + products.size() + " products.");

			return products;
		} catch(Exception e) {
			e.printStackTrace();

			return null;
		}
	}

	private static void writeProductConfigEntriesToFile(StripeConfigEntry stripeConfigEntry) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();

			objectMapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get("wasdiConfig.json").toFile(), stripeConfigEntry);
		} catch (JsonProcessingException e) {
			LOGGER.error(e.getMessage());
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
	}

}
