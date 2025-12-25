package com.example.cart.controller;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.cart.entity.Cart;
import com.example.cart.service.CartService;

@RestController
@RequestMapping("/api/carts")
public class CartController {

	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@PostMapping
	public ResponseEntity<Cart> createCart(@RequestBody Map<String, String> body) {
		Long customerId = body.containsKey("customerId") ? Long.parseLong(body.get("customerId")) : null;
		Cart c = cartService.createCart(customerId);
		return ResponseEntity.ok(c);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Cart> getCart(@PathVariable("id") Long 
	id) {
		Optional<Cart> c = cartService.getCart(id);
		return c.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	public static class AddItemRequest {
		public String productId;

		public String name;
		public BigDecimal unitPrice;
		public int quantity;
	}

	@PostMapping("/{id}/items")
	public ResponseEntity<Cart> addItem(@PathVariable("id") Long id, @RequestBody AddItemRequest req) {
		Cart c = cartService.addItem(id, Long.parseLong(req.productId), req.name, req.unitPrice, req.quantity);
		return ResponseEntity.ok(c);
	}

	@DeleteMapping("/{id}/items/{itemId}")
	public ResponseEntity<Void> removeItem(@PathVariable("id") Long id, @PathVariable("itemId") Long itemId) {
		cartService.removeItem(id, itemId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/checkout")
	public ResponseEntity<Map<String, String>> checkout(@PathVariable("id") Long id) {
		Long orderId = cartService.checkout(id);
		return ResponseEntity.ok(Map.of("orderId", orderId.toString()));
	}
}
