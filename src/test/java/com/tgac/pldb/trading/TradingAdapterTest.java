package com.tgac.pldb.trading;
import com.tgac.monads.Reference;
import com.tgac.monads.pldb.Database;
import com.tgac.monads.pldb.ImmutableDatabase;
import com.tgac.monads.pldb.trading.platform.TradingMessage;
import io.reactivex.subjects.UnicastSubject;
import org.junit.Test;
public class TradingAdapterTest {

	private static final Reference<Database> db = Reference.to(ImmutableDatabase.empty());
	private static final UnicastSubject<TradingMessage> requests = UnicastSubject.create();

	@Test
	public void tst() {

	}
}
