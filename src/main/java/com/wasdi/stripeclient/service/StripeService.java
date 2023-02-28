package com.wasdi.stripeclient.service;

import java.util.List;
import java.util.Map;

import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentLink;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;

public interface StripeService {

	Product createProduct(String id, String name, String description, Map<String, String> metadata, List<String> images) throws StripeException;

	Price createPrice(String productId, String currency, Long unitAmount) throws StripeException;

	Session createSession(String priceId) throws StripeException;

	PaymentLink createPaymentLink(String priceId) throws StripeException;

	Session retrieveSession(String checkoutSessionId) throws StripeException;

	PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException;

	Invoice retrieveInvoice(String invoiceId) throws StripeException;

}
