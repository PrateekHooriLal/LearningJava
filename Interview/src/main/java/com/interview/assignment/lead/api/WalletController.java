package com.interview.assignment.lead.api;

import com.interview.assignment.lead.api.dto.TransferRequest;
import com.interview.assignment.lead.api.dto.TransferResponse;
import com.interview.assignment.lead.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * =============================================================================
 * WalletController — REST API Layer (Presentation Layer)
 * =============================================================================
 *
 * ROLE IN THE ARCHITECTURE:
 *   The controller is the entry point for HTTP requests. It ONLY handles:
 *   1. HTTP concern: parsing request, setting response status.
 *   2. Validation: triggers @Valid constraint checking on the DTO.
 *   3. Delegation: calls the service — NO business logic here.
 *
 * "FAT CONTROLLER" ANTI-PATTERN: Never put business logic in a controller.
 *   If a controller method has more than 5 lines of logic, something is wrong.
 *   Controllers should be thin — they coordinate, not calculate.
 *
 * @RestController = @Controller + @ResponseBody
 *   Tells Spring this class handles REST requests and all return values are
 *   serialized directly to the HTTP response body as JSON (via Jackson).
 *
 * @RequestMapping("/api/wallets"):
 *   All endpoints in this controller are prefixed with /api/wallets.
 *   This follows REST resource naming — "wallets" is the resource noun.
 *
 * INTERVIEW TALKING POINT:
 *   "What HTTP status should a successful transfer return — 200 or 201?"
 *   → 201 Created is for resource creation (POST /users → creates a user).
 *     A transfer doesn't "create" a wallet — it modifies existing resources.
 *     200 OK is appropriate here. Some teams use 202 Accepted for async transfers.
 *
 * COMMON FOLLOW-UP: "How do you handle validation errors?"
 *   → @Valid triggers Bean Validation. If a constraint fails, Spring throws
 *     MethodArgumentNotValidException which maps to 400 Bad Request.
 *     In production, you'd add a @ControllerAdvice / @ExceptionHandler to
 *     return a structured error body (instead of Spring's default ugly JSON).
 *
 * =============================================================================
 */
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    /**
     * Injected via constructor (Lombok @RequiredArgsConstructor generates this).
     * Typed to the interface — controller doesn't know or care about TransferServiceImpl.
     * This is Dependency Inversion: high-level module (controller) depends on abstraction.
     */
    private final TransferService transferService;

    /**
     * POST /api/wallets/transfer
     *
     * WHY POST AND NOT PUT?
     * - PUT is idempotent: calling it twice should produce the same result.
     * - A transfer is NOT idempotent: calling it twice would transfer money twice.
     * - POST is the right verb for non-idempotent state-changing operations.
     *
     * @Valid: Triggers Bean Validation on the TransferRequest DTO.
     *   If fromWalletId is null, Spring returns 400 with an error body before
     *   even entering this method. Keeps validation at the boundary.
     *
     * @RequestBody: Tells Spring to deserialize the HTTP request body (JSON) into
     *   a TransferRequest object using Jackson ObjectMapper.
     *
     * ResponseEntity<TransferResponse>:
     *   Wraps the response, giving you control over HTTP status code and headers.
     *   ResponseEntity.ok(body) → HTTP 200 with body serialized as JSON.
     *
     * ERROR HANDLING (production pattern — not implemented here but interview-relevant):
     *   In production, TransferServiceImpl can throw:
     *     - RuntimeException("Insufficient balance")    → should map to 422 Unprocessable Entity
     *     - OptimisticLockException (concurrency)       → should map to 409 Conflict + client retries
     *     - RuntimeException("Wallet not found")        → should map to 404 Not Found
     *   A @ControllerAdvice class would catch these and return structured error responses.
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("Received transfer request: from wallet {} to wallet {} for amount {}",
                request.getFromWalletId(), request.getToWalletId(), request.getAmount());

        // Delegate entirely to the service — controller stays thin
        TransferResponse response = transferService.transfer(
                request.getFromWalletId(),
                request.getToWalletId(),
                request.getAmount()
        );

        // HTTP 200 OK with the transfer result as JSON in the response body
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/wallets/rate?from=USD&to=INR
     *
     * Utility endpoint — lets clients preview the exchange rate before transferring.
     * In production, you'd also return the rate's timestamp and expiry to help clients
     * decide if they want to proceed.
     *
     * @RequestParam: Maps query string parameters to method arguments.
     *   /rate?from=USD&to=INR → fromCurrency="USD", toCurrency="INR"
     */
    @GetMapping("/rate")
    public ResponseEntity<String> getExchangeRate(
            @RequestParam String from,
            @RequestParam String to) {

        var rate = transferService.getExchangeRate(from, to);
        return ResponseEntity.ok("1 " + from + " = " + rate + " " + to);
    }
}
