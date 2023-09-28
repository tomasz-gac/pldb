package com.tgac.pldb.trading.mapping;
import com.tgac.monads.exceptions.Exceptions;
import com.tgac.monads.pldb.relations.Fact;
import com.tgac.monads.pldb.trading.exchange.Created;
import com.tgac.monads.pldb.trading.exchange.ExchangeMessage;
import com.tgac.monads.pldb.trading.exchange.Trade;
import com.tgac.monads.pldb.trading.logic.Schema;
import com.tgac.monads.reflection.Types;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReplyMapper {

	public static Fact map(ExchangeMessage msg){
		return Types.cast(msg, Created.class)
				.map(ReplyMapper::mapCreated)
				.orElse(() -> Types.cast(msg, Trade.class)
						.map(ReplyMapper::mapTrade))
				.getOrElseThrow(Exceptions.format(IllegalAccessError::new, "Unknown message type: %s", msg));
	}

	private static Fact mapCreated(Created message){
		return Schema.created.fact(
				message.getClientOrderId(),
				message.getExternalId(),
				message.getOccurredAt());
	}

	private static Fact mapTrade(Trade message){
		return Schema.trade.fact(
				message.getId(),
				message.getOrderId(),
				message.getPrice(),
				message.getQuantity(),
				message.getOccurredAt());
	}
}
