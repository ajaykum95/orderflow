package com.orderflow.order.it;

import com.orderflow.order.catalog.services.ProductService;
import com.orderflow.order.it.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIT extends IntegrationTestBase {

    @Autowired ProductService productService;

    @Test
    void adminCreatesProductAndAnyoneCanReadIt() throws Exception {
        String admin = adminToken();
        String name = "Phone-" + uid();
        long id = createProduct(admin, name, "500.00", 10);

        // बिना token के भी दिखता है
        mvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.stock").value(10));
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void customerCannotCreateProducts() throws Exception {
        mvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(customerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "X", "price", new BigDecimal("10.00"), "stock", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidProductIsRejectedWithAListOfProblems() throws Exception {
        mvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "", "price", new BigDecimal("-5"), "stock", -1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void deletedProductDisappears() throws Exception {
        String admin = adminToken();
        long id = createProduct(admin, "Gone-" + uid(), "10.00", 1);

        mvc.perform(delete("/api/admin/products/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
    }

    // इस पूरे project की सबसे ज़रूरी जाँचों में से एक: stock कभी माइनस नहीं होना चाहिए
    @Test
    void concurrentReservationsNeverOversell() throws Exception {
        long id = createProduct(adminToken(), "Scarce-" + uid(), "100.00", 5);
        int buyers = 12;

        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            results.add(pool.submit(() -> {
                startTogether.await();                    // सब एक साथ दौड़ें
                try {
                    productService.reserveStock(id, 1);
                    return true;                          // मिल गया
                } catch (ResponseStatusException e) {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    return false;                         // stock ख़त्म
                }
            }));
        }
        startTogether.countDown();

        int succeeded = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) succeeded++;
        }
        pool.shutdown();

        assertThat(succeeded).as("कितनों को माल मिला").isEqualTo(5);
        assertThat(stockOf(id)).as("बचा हुआ stock").isZero();
    }

    @Test
    void releasedStockComesBack() throws Exception {
        long id = createProduct(adminToken(), "Return-" + uid(), "100.00", 5);

        productService.reserveStock(id, 3);
        assertThat(stockOf(id)).isEqualTo(2);

        productService.releaseStock(id, 3);
        assertThat(stockOf(id)).isEqualTo(5);
    }
}
