package com.vlad.healthbeauty.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlad.healthbeauty.model.Product;
import com.vlad.healthbeauty.model.Role;
import com.vlad.healthbeauty.model.User;
import com.vlad.healthbeauty.repository.ProductRepository;
import com.vlad.healthbeauty.repository.RoleRepository;
import com.vlad.healthbeauty.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        ensureUserWithRole("admin", "admin2026", "ROLE_ADMIN");
        ensureUserWithRole("staff", "staff2026", "ROLE_STAFF");
        ensureUserWithRole("viewer", "viewer2026", "ROLE_USER");
    }

    @Test
    void getAllProducts_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    void getLowStockProducts_shouldReturnOnlyProductsBelowOrEqualToThreshold() throws Exception {
        seedProduct("Low Stock Product", 4);
        seedProduct("Healthy Stock Product", 25);

        mockMvc.perform(get("/api/products/low-stock")
                        .param("threshold", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Low Stock Product"));
    }

    @Test
    void createProduct_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductPayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_withStaff_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("staff", "staff2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_withAdmin_shouldReturn201() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Serum"));
    }

    @Test
    void updateProduct_withStaff_shouldReturn200() throws Exception {
        Product product = seedProduct("Old Name");
        String payload = objectMapper.writeValueAsString(Map.of(
                "name", "Updated Name",
                "price", 31.5,
                "quantity", 15
        ));

        mockMvc.perform(put("/api/products/{id}", product.getId())
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("staff", "staff2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void deleteProduct_withStaff_shouldReturn403() throws Exception {
        Product product = seedProduct("Cannot Delete");

        mockMvc.perform(delete("/api/products/{id}", product.getId())
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("staff", "staff2026")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_withAdmin_shouldReturn204() throws Exception {
        Product product = seedProduct("Can Delete");

        mockMvc.perform(delete("/api/products/{id}", product.getId())
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin2026")))
                .andExpect(status().isNoContent());
    }

    @Test
    void createProduct_withInvalidJsonType_shouldReturn400() throws Exception {
        String invalidPayload = """
                {
                  "name": "Broken Product",
                  "brand": "Test Brand",
                  "category": "Skin",
                  "price": "not-a-number",
                  "quantity": 5
                }
                """;

        mockMvc.perform(post("/api/products")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_withNegativePrice_shouldReturn400() throws Exception {
        String invalidPayload = objectMapper.writeValueAsString(Map.of(
                "name", "Invalid Price Product",
                "brand", "Test Brand",
                "category", "Skin",
                "price", -5.0,
                "quantity", 5
        ));

        mockMvc.perform(post("/api/products")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.price").value("Price must be positive"));
    }

    @Test
    void createProduct_withBlankName_shouldReturn400() throws Exception {
        String invalidPayload = objectMapper.writeValueAsString(Map.of(
                "name", "   ",
                "brand", "Test Brand",
                "category", "Skin",
                "price", 12.0,
                "quantity", 5
        ));

        mockMvc.perform(post("/api/products")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Product name is required"));
    }

    private void ensureUserWithRole(String username, String rawPassword, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(roleName);
            return roleRepository.save(newRole);
        });

        User user = userRepository.findByUsername(username).orElseGet(User::new);
        user.setUsername(username);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(Set.of(role));
        userRepository.save(user);
    }

    private Product seedProduct(String name) {
        return seedProduct(name, 10);
    }

    private Product seedProduct(String name, int quantity) {
        Product product = new Product();
        product.setName(name);
        product.setBrand("Brand X");
        product.setCategory("Skin");
        product.setPrice(new BigDecimal("19.99"));
        product.setStockQuantity(quantity);
        product.setLowStockThreshold(2);
        product.setActive(true);
        return productRepository.save(product);
    }

    private String validProductPayload() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "Test Serum",
                "brand", "Test Brand",
                "category", "Skin",
                "price", 25.99,
                "quantity", 12
        );
        return objectMapper.writeValueAsString(payload);
    }
}
