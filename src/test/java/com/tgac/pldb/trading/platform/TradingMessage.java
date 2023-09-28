package com.tgac.pldb.trading.platform;
import java.time.ZonedDateTime;
public interface TradingMessage {
	String getClientOrderId();
	ZonedDateTime getOccurredAt();
}
