package com.tgac.pldb.trading.exchange;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Value
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Trade implements ExchangeMessage{
	String id;
	String orderId;
	BigDecimal price;
	BigDecimal quantity;
	ZonedDateTime occurredAt;
}
