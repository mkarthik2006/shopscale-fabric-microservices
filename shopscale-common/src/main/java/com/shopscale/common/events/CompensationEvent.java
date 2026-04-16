package com.shopscale.common.events;

import java.util.UUID;

public record CompensationEvent(UUID orderId) {}