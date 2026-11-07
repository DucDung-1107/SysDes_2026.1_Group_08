package vn.medora.api.finance;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class FinanceDtos {
    private FinanceDtos() { }
    public record PriceEnd(@NotNull Instant effectiveTo) { }
    public record PriceInput(@NotNull UUID serviceId,
        @NotNull @DecimalMin("1") @Digits(integer=15,fraction=0) BigDecimal amount,
        @NotNull @Pattern(regexp="VND") String currency,@NotNull Instant effectiveFrom,Instant effectiveTo) { }
    public record DiscountInput(@NotNull @DecimalMin("0") @Digits(integer=15,fraction=0) BigDecimal amount,
        @NotBlank @Size(max=500) String reason,@NotNull @PositiveOrZero Long version) { }
    public record PaymentInput(@NotNull @DecimalMin("1") @Digits(integer=15,fraction=0) BigDecimal amount,
        @NotNull @Pattern(regexp="VND") String currency,@NotNull @Pattern(regexp="CASH|BANK_TRANSFER") String method,
        @NotNull Boolean receivedVerified) { }
    public record RefundInput(@NotNull UUID paymentId,@NotNull @DecimalMin("1") @Digits(integer=15,fraction=0) BigDecimal amount,
        @NotBlank @Size(max=500) String reason) { }
    public record ApprovalInput(@NotNull @PositiveOrZero Long version,@NotNull @Pattern(regexp="APPROVE|REJECT") String action,
        @NotBlank @Size(max=500) String reason) { }
    public record PolicyInput(@NotNull @DecimalMin("0") @Digits(integer=15,fraction=0) BigDecimal autoRefundLimit,
        @NotNull Boolean dischargeApprovalRequired,@NotNull @PositiveOrZero Long version) { }
}
