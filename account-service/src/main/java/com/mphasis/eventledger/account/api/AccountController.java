package com.mphasis.eventledger.account.api;

import com.mphasis.eventledger.account.service.AccountTransactionService;
import com.mphasis.eventledger.account.service.ApplyTransactionCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.mphasis.eventledger.account.error.TraceSupport.responseTraceId;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountTransactionService service;

    public AccountController(AccountTransactionService service) {
        this.service = service;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<ApplyTransactionResponse> applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody ApplyTransactionRequest request,
            @RequestHeader(value = "X-Trace-ID", required = false) String traceIdHeader
    ) {
        var traceId = responseTraceId(traceIdHeader);
        var result = service.apply(accountId, new ApplyTransactionCommand(
                request.eventId(),
                request.accountId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.eventTimestamp(),
                traceId
        ));
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .header("X-Trace-ID", traceId)
                .body(ApplyTransactionResponse.from(result, traceId));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> balance(
            @PathVariable String accountId,
            @RequestHeader(value = "X-Trace-ID", required = false) String traceIdHeader
    ) {
        var traceId = responseTraceId(traceIdHeader);
        return ResponseEntity.ok()
                .header("X-Trace-ID", traceId)
                .body(BalanceResponse.from(service.getAccount(accountId), traceId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> account(
            @PathVariable String accountId,
            @RequestHeader(value = "X-Trace-ID", required = false) String traceIdHeader
    ) {
        var traceId = responseTraceId(traceIdHeader);
        return ResponseEntity.ok()
                .header("X-Trace-ID", traceId)
                .body(AccountResponse.from(service.getAccount(accountId), traceId));
    }
}
