package com.tgac.pldb.trading.logic;

import com.tgac.monads.pldb.relations.Property;
import com.tgac.monads.pldb.relations.Relations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static com.tgac.monads.pldb.relations.Relations.relation;
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Schema {
	// PLATFORM MESSAGES

	public static final Property<String> clientOrderId = Property.of("clientOrderId");
	public static final Property<String> externalOrderId = Property.of("externalOrderId");
	public static final Property<BigDecimal> price = Property.of("price");
	public static final Property<BigDecimal> quantity = Property.of("quantity");
	public static final Property<ZonedDateTime> occurredAt = Property.of("ocurredAt");

	public static final Property<BigDecimal> avgPrice = Property.of("avgPrice");
	public static final Property<BigDecimal> leavesQty = Property.of("avgPrice");


	public static final Relations._4<String, BigDecimal, BigDecimal, ZonedDateTime> orderSingle =
			relation("orderSingle", clientOrderId.indexed(), price, quantity, occurredAt);

	public static final Relations._3<String, String, ZonedDateTime> reportNew =
			relation("reportNew", clientOrderId.indexed(), externalOrderId, occurredAt);

	public static final Relations._6<String, BigDecimal, BigDecimal, BigDecimal, BigDecimal, ZonedDateTime> reportPartiallyFilled =
			relation("reportPartiallyFilled",
					clientOrderId.indexed(),
					price, quantity, avgPrice, leavesQty, occurredAt);

	public static final Relations._6<String, BigDecimal, BigDecimal, BigDecimal, BigDecimal, ZonedDateTime> reportFilled =
			relation("reportFilled",
					clientOrderId.indexed(),
					price, quantity, avgPrice, leavesQty, occurredAt);

	// EXCHANGE MESSAGES

	public static final Property<String> tradeId = Property.of("tradeId");
	public static final Relations._3<String, String, ZonedDateTime> created =
			relation("created", externalOrderId.indexed(), clientOrderId.indexed(), occurredAt);
	public static final Relations._5<String, String, BigDecimal, BigDecimal, ZonedDateTime> trade =
			relation("trade", tradeId.indexed(), externalOrderId.indexed(), price, quantity, occurredAt);


}
