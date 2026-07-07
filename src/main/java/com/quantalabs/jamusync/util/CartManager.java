package com.quantalabs.jamusync.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

// A very simple in-memory shopping cart.
// Everything is static so the cart is shared across all guest screens.
// There is no database here - the cart only lives while the app is open.
public class CartManager {

    // One line in the cart: a product name, its price, and how many.
    public static class CartItem {
        private String productName;
        private double price;
        private int quantity;

        public CartItem(String productName, double price, int quantity) {
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
        }

        public String getProductName() {
            return productName;
        }

        public double getPrice() {
            return price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        // The price for this line (price times how many).
        public double getSubtotal() {
            return price * quantity;
        }
    }

    // The list of items in the cart. It is shared by every screen.
    private static final ObservableList<CartItem> items = FXCollections.observableArrayList();

    // Add an item. If the same product is already in the cart, just add more.
    public static void addItem(String name, double price, int qty) {
        for (CartItem item : items) {
            if (item.getProductName().equals(name)) {
                item.setQuantity(item.getQuantity() + qty);
                return;
            }
        }
        items.add(new CartItem(name, price, qty));
    }

    // Remove the item at the given position in the list.
    public static void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    // Get the whole cart list.
    public static ObservableList<CartItem> getItems() {
        return items;
    }

    // The total price of everything in the cart.
    public static double getTotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    // How many items are in the cart (adds up all the quantities).
    public static int getItemCount() {
        int count = 0;
        for (CartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    // Empty the cart.
    public static void clear() {
        items.clear();
    }
}
