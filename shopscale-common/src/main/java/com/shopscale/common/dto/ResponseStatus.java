package com.shopscale.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Response Status Enum", description = "Predefined execution states for API responses.")
public enum ResponseStatus {
    
    @Schema(description = "API operation completed successfully without errors.")
    SUCCESS,
    
    @Schema(description = "API operation failed due to business logic or server error.")
    FAILURE
}
