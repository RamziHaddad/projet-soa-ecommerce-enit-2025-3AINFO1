package com.example.cart.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cart.entity.Cart;
import com.example.cart.entity.CartItem;
import com.example.cart.entity.OrdersOutbox;
import com.example.cart.repository.CartItemRepository;
import com.example.cart.repository.CartRepository;
import com.example.cart.repository.OrdersOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final OrdersOutboxRepository outboxRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, OrdersOutboxRepository outboxRepository) {
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.outboxRepository = outboxRepository;
	}

	@Transactional
	public Cart createCart(Long customerId) {
		Cart c = new Cart();
		c.setCustomerId(customerId);
		c.setStatus("OPEN");
		c.setCurrency("TND");
		c.setTotalAmount(BigDecimal.ZERO);
		return cartRepository.save(c);
	}

	@Transactional(readOnly = true)
	public Optional<Cart> getCart(Long cartId) {
		return cartRepository.findById(cartId);
	}

	@Transactional
	public Cart addItem(Long cartId, Long productId, String name, BigDecimal unitPrice, int quantity) {
		Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new NoSuchElementException("Cart not found"));

		// try to find existing item
		List<CartItem> items = cartItemRepository.findByCartId(cartId);
		Optional<CartItem> existing = items.stream().filter(i -> productId.equals(i.getProductId())).findFirst();
		CartItem item;
		if (existing.isPresent()) {
			item = existing.get();
			item.setQuantity(item.getQuantity() + quantity);
			item.setUnitPrice(unitPrice);
		} else {
			item = new CartItem();
			item.setCart(cart);
			item.setProductId(productId);
			item.setName(name);
			item.setUnitPrice(unitPrice);
			item.setQuantity(quantity);
		}
		// lineTotal will be computed in entity before persist
		cartItemRepository.save(item);

		// recalc cart total
		BigDecimal total = cartItemRepository.findByCartId(cartId).stream()
				.map(CartItem::getLineTotal)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		cart.setTotalAmount(total);
		cartRepository.save(cart);
		return cart;
	}

	@Transactional
	public void removeItem(Long cartId, Long itemId) {
		Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new NoSuchElementException("Cart not found"));
		CartItem item = cartItemRepository.findById(itemId).orElseThrow(() -> new NoSuchElementException("Item not found"));
		if (!cart.getId().equals(item.getCart().getId())) throw new IllegalStateException("Item does not belong to cart");
		cartItemRepository.delete(item);

		// recalc
		BigDecimal total = cartItemRepository.findByCartId(cartId).stream()
				.map(CartItem::getLineTotal)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		cart.setTotalAmount(total);
		cartRepository.save(cart);
	}

	@Transactional
	public Long checkout(Long cartId) {
		Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new NoSuchElementException("Cart not found"));
		if (cart.getItems() == null || cart.getItems().isEmpty()) throw new IllegalStateException("Cart is empty");

		Long orderId = Math.abs(new Random().nextLong());

		// Build lightweight payload
		Map<String, Object> payload = new HashMap<>();
		payload.put("orderId", orderId.toString());
		payload.put("cartId", cart.getId().toString());
		payload.put("customerId", cart.getCustomerId() == null ? null : cart.getCustomerId().toString());
		List<Map<String, Object>> items = new ArrayList<>();
		for (CartItem ci : cartItemRepository.findByCartId(cartId)) {
			Map<String, Object> it = new HashMap<>();
			it.put("productId", ci.getProductId().toString());
			it.put("name", ci.getName());
			it.put("unitPrice", ci.getUnitPrice());
			it.put("quantity", ci.getQuantity());
			items.add(it);
		}
		payload.put("items", items);
		payload.put("totalAmount", cart.getTotalAmount());
		payload.put("createdAt", OffsetDateTime.now().toString());

		try {
			String json = objectMapper.writeValueAsString(payload);
			OrdersOutbox out = new OrdersOutbox();
			out.setAggregateType("order");
			out.setAggregateId(orderId);
			out.setEventType("OrderCreated");
			out.setPayload(json);
			outboxRepository.save(out);
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize outbox payload", e);
		}

		cart.setStatus("CHECKED_OUT");
		cartRepository.save(cart);
		return orderId;
	}
}
