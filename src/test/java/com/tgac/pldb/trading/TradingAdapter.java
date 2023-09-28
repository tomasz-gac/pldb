package com.tgac.pldb.trading;

import com.tgac.monads.Reference;
import com.tgac.monads.functional.Tuples;
import com.tgac.monads.pldb.Database;
import com.tgac.monads.pldb.ImmutableDatabase;
import com.tgac.monads.pldb.events.FactChangedEvent;
import com.tgac.monads.pldb.relations.Fact;
import com.tgac.monads.pldb.trading.exchange.ExchangeMessage;
import com.tgac.monads.pldb.trading.mapping.ReplyMapper;
import com.tgac.monads.pldb.trading.mapping.RequestMapper;
import com.tgac.monads.pldb.trading.platform.Reject;
import com.tgac.monads.pldb.trading.platform.TradingMessage;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.UnicastSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.tgac.monads.functional.Tuples.tuple;
import static com.tgac.monads.pldb.trading.logic.Schema.orderSingle;
@Slf4j
@RequiredArgsConstructor
public class TradingAdapter {
	private final Observable<TradingMessage> requests;
	private final Consumer<TradingMessage> responses;
	private Exchange exchange;

	private final Reference<Database> db = Reference.to(ImmutableDatabase.empty());

	private final CompositeDisposable disposable = new CompositeDisposable();
	private final UnicastSubject<TradingMessage> rejectStream = UnicastSubject.create();

	@PostConstruct
	public void init() {
		disposable.add(
				requests.map(req -> tuple(req.getClientOrderId(), RequestMapper.map(req)))
						.subscribe(this::processRequest, t -> log.error("Request stream failed: ", t)));

		disposable.add(
				Observable.wrap(exchange)
						.map(res -> tuple(res, ReplyMapper.map(res)))
						.subscribe(this::processReply, t -> log.error("Reply stream failed: ", t)));

		db.update(db -> db.observe(orderSingle
				.observer(FactChangedEvent.FACT_ADDED,
						(clientOrderId, price, quantity, occurredAt) ->
								sendOrderSingle(clientOrderId, price, quantity))));

	}

	private void processRequest(Tuples._2<String, Fact> clOrdIdAndFact) {
		db.update(db -> db
				.facts(Collections.singletonList(clOrdIdAndFact._1))
				.mapError(s -> new IllegalArgumentException(s.collect(Collectors.joining(", "))))
				.toTry()
				.onFailure(e -> rejectStream.onNext(Reject.of(clOrdIdAndFact._0, ZonedDateTime.now(), e.getMessage())))
				.getOrElse(db));
	}

	private void processReply(Tuples._2<ExchangeMessage, Fact> msgAndFact) {
		db.update(db -> db
				.facts(Collections.singletonList(msgAndFact._1))
				.mapError(s -> new IllegalArgumentException(s.collect(Collectors.joining(", "))))
				.toTry()
				.get());
	}

	@PreDestroy
	public void cleanup() {
		disposable.dispose();
	}

	private void sendOrderSingle(String clientOrderId, BigDecimal price, BigDecimal quantity) {
		exchange.orderSingleLimit(price, quantity)
				.thenAccept(success -> {
					if (!success) {
						rejectStream.onNext(Reject.of(clientOrderId, ZonedDateTime.now(), "Sending order single failed"));
					}
				});
	}

}
