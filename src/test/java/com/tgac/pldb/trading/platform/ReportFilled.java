package com.tgac.pldb.trading.platform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
@Value
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class ReportFilled implements TradingMessage {
	String clientOrderId;
	ZonedDateTime occurredAt;

	BigDecimal price;
	BigDecimal quantity;
	BigDecimal avgPrice;
}
