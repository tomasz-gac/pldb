package com.tgac.pldb.trading;
import com.tgac.monads.pldb.trading.exchange.ExchangeMessage;
import io.reactivex.ObservableSource;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
public interface Exchange extends ObservableSource<ExchangeMessage> {
	CompletableFuture<Boolean> orderSingleLimit(BigDecimal price, BigDecimal size);

}
