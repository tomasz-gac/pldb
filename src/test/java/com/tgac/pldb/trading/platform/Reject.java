package com.tgac.pldb.trading.platform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Reject implements TradingMessage {
	String clientOrderId;
	ZonedDateTime occurredAt;
	String reason;
}
