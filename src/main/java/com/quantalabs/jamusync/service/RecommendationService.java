package com.quantalabs.jamusync.service;

import com.quantalabs.jamusync.dao.ProductDAO;
import com.quantalabs.jamusync.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RecommendationService {

    private final ProductDAO productDAO = new ProductDAO();

    /**
     * Recommend products based on keywords related to health concerns.
     */
    public List<Product> recommendByKeywords(String query) {
        if (query == null || query.trim().isEmpty()) {
            return productDAO.getAllActiveProducts();
        }

        String lower = query.toLowerCase(Locale.ROOT);
        List<Product> all = productDAO.getAllActiveProducts();
        List<Product> matched = new ArrayList<>();

        for (Product product : all) {
            if (matchesProduct(product, lower)) {
                matched.add(product);
            }
        }

        if (matched.isEmpty()) {
            return all.stream().limit(3).collect(Collectors.toList());
        }
        return matched;
    }

    /**
     * Recommend products for a specific health benefit filter.
     */
    public List<Product> filterByBenefit(String benefit) {
        if (benefit == null || benefit.isEmpty() || "All".equalsIgnoreCase(benefit)) {
            return productDAO.getAllActiveProducts();
        }
        String lower = benefit.toLowerCase(Locale.ROOT);
        return productDAO.getAllActiveProducts().stream()
            .filter(p -> matchesProduct(p, lower))
            .collect(Collectors.toList());
    }

    /**
     * Search products by name, description, ingredients, or health benefits.
     */
    public List<Product> searchProducts(String query, String benefitFilter) {
        List<Product> products = filterByBenefit(benefitFilter);
        if (query == null || query.trim().isEmpty()) {
            return products;
        }
        String lower = query.toLowerCase(Locale.ROOT);
        return products.stream()
            .filter(p -> matchesProduct(p, lower))
            .collect(Collectors.toList());
    }

    public String buildProductContext() {
        List<Product> products = productDAO.getAllActiveProducts();
        StringBuilder sb = new StringBuilder("Available Jamu products:\n");
        for (Product p : products) {
            sb.append("- ").append(p.getName());
            if (p.getHealthBenefits() != null && !p.getHealthBenefits().isEmpty()) {
                sb.append(" (Benefits: ").append(p.getHealthBenefits()).append(")");
            }
            if (p.getIngredients() != null && !p.getIngredients().isEmpty()) {
                sb.append(" [Ingredients: ").append(p.getIngredients()).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private boolean matchesProduct(Product product, String lower) {
        return contains(product.getName(), lower)
            || contains(product.getDescription(), lower)
            || contains(product.getIngredients(), lower)
            || contains(product.getHealthBenefits(), lower);
    }

    private boolean contains(String field, String lower) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(lower);
    }
}
