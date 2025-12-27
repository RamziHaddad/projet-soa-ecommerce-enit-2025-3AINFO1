package com.example.cart.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cart.client.OrderClient;
import com.example.cart.dto.OrderRequest;
import com.example.cart.dto.OrderResponse;
import com.example.cart.entity.Cart;
import com.example.cart.entity.CartItem;
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
    private final OrderClient orderClient;

    public CartService(CartRepository cartRepository,
                   CartItemRepository cartItemRepository,
                   OrdersOutboxRepository outboxRepository,
                   OrderClient orderClient) {
    this.cartRepository = cartRepository;
    this.cartItemRepository = cartItemRepository;
    this.outboxRepository = outboxRepository;
    this.orderClient = orderClient;
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
    public OrderResponse checkout(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new NoSuchElementException("Cart not found"));

       if (cart.getItems() == null || cart.getItems().isEmpty())
        throw new IllegalStateException("Cart is empty");

    // Build OrderRequest
       OrderRequest orderReq = new OrderRequest();
    orderReq.customerId = cart.getCustomerId();
    orderReq.shippingAddress = "Default Address"; // could come from user input

    for (CartItem ci : cartItemRepository.findByCartId(cartId)) {
        OrderRequest.OrderItem item = new OrderRequest.OrderItem();
        item.productId = ci.getProductId();
        item.quantity = ci.getQuantity();
        item.unitPrice = ci.getUnitPrice();
        orderReq.items.add(item);
    }

    // Call Order Service
    OrderResponse response = (OrderResponse) orderClient.createOrder(orderReq);

    // Update cart status
    cart.setStatus("CHECKED_OUT");
    cartRepository.save(cart);

    return response;
}

}
