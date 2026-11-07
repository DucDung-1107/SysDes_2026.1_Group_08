package vn.medora.api.common;

import jakarta.validation.constraints.*;

public record VersionRequest(@NotNull @PositiveOrZero Long version) { }
