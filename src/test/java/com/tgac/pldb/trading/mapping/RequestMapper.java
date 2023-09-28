package com.tgac.pldb.trading.mapping;

import com.tgac.monads.exceptions.Exceptions;
import com.tgac.monads.pldb.relations.Fact;
import com.tgac.monads.pldb.trading.platform.OrderSingle;
import com.tgac.monads.pldb.trading.platform.TradingMessage;
import com.tgac.monads.reflection.Types;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.tgac.monads.pldb.trading.logic.Schema.orderSingle;
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestMapper {
	public static Fact map(TradingMessage message){
		return Types.cast(message, OrderSingle.class)
				.map(os -> orderSingle.fact(
						os.getClientOrderId(),
						os.getPrice(),
						os.getQuantity(),
						os.getOccurredAt()))
				.getOrElseThrow(Exceptions.format(IllegalArgumentException::new, "Unsupported request: %s", message));

	}
}
