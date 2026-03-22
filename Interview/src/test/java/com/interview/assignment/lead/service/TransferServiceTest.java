package com.interview.assignment.lead.service;

import com.interview.assignment.lead.domain.Wallet;
import com.interview.assignment.lead.infrastructure.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TransferServiceTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private WalletRepository walletRepository;

    private Long wallet1Id;
    private Long wallet2Id;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
        Wallet w1 = Wallet.builder()
                .userId(1L)
                .currency("USD")
                .balance(new BigDecimal("1000.00"))
                .build();
        Wallet w2 = Wallet.builder()
                .userId(2L)
                .currency("USD")
                .balance(new BigDecimal("500.00"))
                .build();
        
        wallet1Id = walletRepository.save(w1).getId();
        wallet2Id = walletRepository.save(w2).getId();
    }

    @Test
    void testSuccessfulTransfer() {
        transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("100.00"));

        Wallet w1 = walletRepository.findById(wallet1Id).get();
        Wallet w2 = walletRepository.findById(wallet2Id).get();

        assertEquals(0, new BigDecimal("900.00").compareTo(w1.getBalance()));
        assertEquals(0, new BigDecimal("600.00").compareTo(w2.getBalance()));
    }

    @Test
    void testConcurrentTransferOptimisticLocking() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Simulate two threads trying to transfer from the same wallet at the same time
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("100.00"));
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Unexpected error: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // One should succeed, one should fail due to optimistic lock
        assertEquals(1, successCount.get(), "Only one transfer should succeed");
        assertEquals(1, failureCount.get(), "One transfer should fail due to optimistic locking");
    }

    @Test
    void testInsufficientBalance() {
        assertThrows(RuntimeException.class, () -> {
            transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("2000.00"));
        });
    }
}
