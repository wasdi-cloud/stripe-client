package com.wasdi.stripeclient.service;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentLink;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class StripeServiceImpl implements StripeService {

	private static Logger LOGGER = LoggerFactory.getLogger(StripeServiceImpl.class);

	@Value("${stripe.api.key}")
	private String stripeApiKey;

	@Value("${wasdi.callback.url}")
	private String wasdiCallbackUrl;

	@Value("${wasdi.logo.url}")
	private String wasdiLogoUrl;

	@Value("${wasdi.marketplace.url}")
	private String wasdiMarketplaceUrl;

	@Value("${local.img.base.url}")
	private String localImgBaseUrl;

	@PostConstruct
	public void init() {
		LOGGER.info("stripeApiKey: " + this.stripeApiKey);
		Stripe.apiKey = stripeApiKey;
	}

	@Override
	public Product createProduct(String id, String name, String description, Map<String, String> metadata, List<String> images)
			throws StripeException {
		LOGGER.info("createProduct");

		ProductCreateParams.Builder builder = ProductCreateParams
				.builder()
				.setId(id)
				.setName(name)
				.setType(ProductCreateParams.Type.SERVICE)
				.setDescription(description);

		if (images != null && !images.isEmpty()) {
			images.forEach((String image) -> {
				if (!image.isEmpty()) {
					builder.addImage(image);
				}
			});
		}

		if (metadata != null && !metadata.isEmpty()) {
			builder.putAllMetadata(metadata);
		}

		ProductCreateParams productParams = builder.build();

		Product product = Product.create(productParams);

		return product;
	}

	@Override
	public Price createPrice(String productId, String currency, Long unitAmount) throws StripeException {
		LOGGER.info("createPrice");

		PriceCreateParams params = PriceCreateParams
				.builder()
				.setProduct(productId)
				.setCurrency(currency)
				.setUnitAmount(unitAmount * 100)
				.build();

		Price price = Price.create(params);

		return price;
	}

	@Override
	public Session createSession(String priceId) throws StripeException {
		SessionCreateParams params = SessionCreateParams
				.builder()
				.setMode(SessionCreateParams.Mode.PAYMENT)
				.build();

		Session session = Session.create(params);

		return session;
	}

	@Override
	public PaymentLink createPaymentLink(String priceId) throws StripeException {
		LOGGER.info("createPaymentLink");

		PaymentLinkCreateParams.LineItem.AdjustableQuantity adjustableQuantity = PaymentLinkCreateParams.LineItem.AdjustableQuantity
				.builder()
				.setEnabled(false)
				.build();

		PaymentLinkCreateParams.LineItem lineItem = PaymentLinkCreateParams.LineItem
				.builder()
				.setPrice(priceId)
				.setQuantity(1L)
				.setAdjustableQuantity(adjustableQuantity)
				.build();

		PaymentLinkCreateParams.AfterCompletion.Redirect redirect = PaymentLinkCreateParams.AfterCompletion.Redirect
				.builder()
				.setUrl(wasdiCallbackUrl)
				.build();

		PaymentLinkCreateParams.AfterCompletion afterCompletion = PaymentLinkCreateParams.AfterCompletion
				.builder()
				.setType(PaymentLinkCreateParams.AfterCompletion.Type.REDIRECT)
				.setRedirect(redirect)
				.build();

		PaymentLinkCreateParams.TaxIdCollection taxIdCollection = PaymentLinkCreateParams.TaxIdCollection
				.builder()
				.setEnabled(true)
				.build();

		PaymentLinkCreateParams params = PaymentLinkCreateParams
				.builder()
				.addLineItem(lineItem)
				.setAfterCompletion(afterCompletion)
				.setTaxIdCollection(taxIdCollection)
				.build();

		PaymentLink paymentLink = PaymentLink.create(params);

		return paymentLink;
	}

	@Override
	public Session retrieveSession(String checkoutSessionId) throws StripeException {
		Session session = Session.retrieve(checkoutSessionId);

		return session;
	}

	@Override
	public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
		LOGGER.info("retrievePaymentIntent");

		PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

		return paymentIntent;
	}

	@Override
	public Invoice retrieveInvoice(String invoiceId) throws StripeException {
		LOGGER.info("retrieveInvoice");

		Invoice invoice = Invoice.retrieve(invoiceId);

		return invoice;
	}

}
