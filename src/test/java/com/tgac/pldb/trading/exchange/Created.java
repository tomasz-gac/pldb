package com.tgac.pldb.trading.exchange;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Created implements ExchangeMessage{
	String clientOrderId;
	String externalId;
	ZonedDateTime occurredAt;
}
