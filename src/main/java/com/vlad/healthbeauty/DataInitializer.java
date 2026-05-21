package com.vlad.healthbeauty;

import com.vlad.healthbeauty.model.Role;
import com.vlad.healthbeauty.model.Product;
import com.vlad.healthbeauty.model.Supplier;
import com.vlad.healthbeauty.model.User;
import com.vlad.healthbeauty.repository.ProductRepository;
import com.vlad.healthbeauty.repository.RoleRepository;
import com.vlad.healthbeauty.repository.SupplierRepository;
import com.vlad.healthbeauty.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@org.springframework.context.annotation.Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(RoleRepository roleRepo,
                               UserRepository userRepo,
                               SupplierRepository supplierRepo,
                               ProductRepository productRepo,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            Role adminRole = roleRepo.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_ADMIN");
                        return roleRepo.save(r);
                    });

            Role staffRole = roleRepo.findByName("ROLE_STAFF")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_STAFF");
                        return roleRepo.save(r);
                    });

            if (userRepo.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRoles(Set.of(adminRole));
                userRepo.save(admin);
            }

            if (userRepo.findByUsername("staff").isEmpty()) {
                User staff = new User();
                staff.setUsername("staff");
                staff.setPassword(passwordEncoder.encode("staff123"));
                staff.setRoles(Set.of(staffRole));
                userRepo.save(staff);
            }

            List<Supplier> suppliers = new ArrayList<>();
            if (supplierRepo.count() == 0) {
                Supplier s1 = new Supplier();
                s1.setName("L'Oreal Distributor");
                s1.setContactName("Maria Pop");
                s1.setEmail("sales@loreal.test");
                s1.setPhone("0712345678");
                supplierRepo.save(s1);
                suppliers.add(s1);

                Supplier s2 = new Supplier();
                s2.setName("Nivea Wholesale");
                s2.setContactName("Andrei Ionescu");
                s2.setEmail("orders@nivea.test");
                s2.setPhone("0722334455");
                supplierRepo.save(s2);
                suppliers.add(s2);
            } else {
                suppliers = supplierRepo.findAll();
            }

            if (productRepo.count() == 0) {
                Supplier firstSupplier = suppliers.isEmpty() ? null : suppliers.get(0);
                Supplier secondSupplier = suppliers.size() > 1 ? suppliers.get(1) : firstSupplier;

                Product p1 = new Product();
                p1.setName("Hydrating Face Serum");
                p1.setCategory("Skincare");
                p1.setBrand("L'Oreal");
                p1.setPrice(new BigDecimal("79.99"));
                p1.setStockQuantity(25);
                p1.setLowStockThreshold(5);
                p1.setSupplier(firstSupplier);
                productRepo.save(p1);

                Product p2 = new Product();
                p2.setName("Vitamin C Day Cream");
                p2.setCategory("Skincare");
                p2.setBrand("Nivea");
                p2.setPrice(new BigDecimal("54.50"));
                p2.setStockQuantity(18);
                p2.setLowStockThreshold(4);
                p2.setSupplier(secondSupplier);
                productRepo.save(p2);

                Product p3 = new Product();
                p3.setName("Repair Hair Mask");
                p3.setCategory("Haircare");
                p3.setBrand("Elseve");
                p3.setPrice(new BigDecimal("42.00"));
                p3.setStockQuantity(12);
                p3.setLowStockThreshold(3);
                p3.setSupplier(firstSupplier);
                productRepo.save(p3);
            }
        };
    }
}

