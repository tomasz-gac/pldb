package com.tgac.pldb.relations;

import com.tgac.functional.Consumers;
import com.tgac.functional.Functions;
import com.tgac.functional.Numbers;
import com.tgac.functional.Tuples;
import com.tgac.logic.Goal;
import com.tgac.logic.LVal;
import com.tgac.logic.Unifiable;
import com.tgac.pldb.Database;
import com.tgac.pldb.events.DatabaseEventListener;
import com.tgac.pldb.events.FactChangedEvent;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.stream.Collectors;

import static com.tgac.functional.Tuples.tuple;
import static com.tgac.logic.LVar.lvar;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Relations {
	public static _0 relation(String name) {
		return new _0(name, tuple());
	}
	public static <T0> _1<T0> relation(String name, Property<T0> v0) {
		return new _1<>(name, tuple(v0));
	}
	public static <T0, T1> _2<T0, T1> relation(String name, Property<T0> v0, Property<T1> v1) {
		return new _2<>(name, tuple(v0, v1));
	}
	public static <T0, T1, T2> _3<T0, T1, T2> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2) {
		return new _3<>(name, tuple(v0, v1, v2));
	}
	public static <T0, T1, T2, T3> _4<T0, T1, T2, T3> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3) {
		return new _4<>(name, tuple(v0, v1, v2, v3));
	}
	public static <T0, T1, T2, T3, T4> _5<T0, T1, T2, T3, T4> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4) {
		return new _5<>(name, tuple(v0, v1, v2, v3, v4));
	}
	public static <T0, T1, T2, T3, T4, T5> _6<T0, T1, T2, T3, T4, T5> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4,
			Property<T5> v5) {
		return new _6<>(name, tuple(v0, v1, v2, v3, v4, v5));
	}
	public static <T0, T1, T2, T3, T4, T5, T6> _7<T0, T1, T2, T3, T4, T5, T6> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4,
			Property<T5> v5, Property<T6> v6) {
		return new _7<>(name, tuple(v0, v1, v2, v3, v4, v5, v6));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7> _8<T0, T1, T2, T3, T4, T5, T6, T7> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3,
			Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7) {
		return new _8<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8> _9<T0, T1, T2, T3, T4, T5, T6, T7, T8> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3,
			Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8) {
		return new _9<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> _10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2,
			Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9) {
		return new _10<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> _11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2,
			Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9, Property<T10> v10) {
		return new _11<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> _12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> relation(String name, Property<T0> v0, Property<T1> v1,
			Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9, Property<T10> v10, Property<T11> v11) {
		return new _12<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> _13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> relation(String name, Property<T0> v0, Property<T1> v1,
			Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9, Property<T10> v10, Property<T11> v11,
			Property<T12> v12) {
		return new _13<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> _14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> relation(String name, Property<T0> v0,
			Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9, Property<T10> v10,
			Property<T11> v11, Property<T12> v12, Property<T13> v13) {
		return new _14<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> _15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> relation(String name,
			Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9, Property<T10> v10,
			Property<T11> v11, Property<T12> v12, Property<T13> v13, Property<T14> v14) {
		return new _15<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> _16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> relation(String name,
			Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9, Property<T10> v10,
			Property<T11> v11, Property<T12> v12, Property<T13> v13, Property<T14> v14, Property<T15> v15) {
		return new _16<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> _17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> relation(
			String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9,
			Property<T10> v10, Property<T11> v11, Property<T12> v12, Property<T13> v13, Property<T14> v14, Property<T15> v15, Property<T16> v16) {
		return new _17<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> _18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> relation(
			String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9,
			Property<T10> v10, Property<T11> v11, Property<T12> v12, Property<T13> v13, Property<T14> v14, Property<T15> v15, Property<T16> v16, Property<T17> v17) {
		return new _18<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> _19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> relation(
			String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9,
			Property<T10> v10, Property<T11> v11, Property<T12> v12, Property<T13> v13, Property<T14> v14, Property<T15> v15, Property<T16> v16, Property<T17> v17, Property<T18> v18) {
		return new _19<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> _20<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> relation(
			String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9,
			Property<T10> v10, Property<T11> v11, Property<T12> v12, Property<T13> v13, Property<T14> v14, Property<T15> v15, Property<T16> v16, Property<T17> v17, Property<T18> v18,
			Property<T19> v19) {
		return new _20<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> _21<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> relation(
			String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9,
			Property<T10> v10, Property<T11> v11, Property<T12> v12, Property<T13> v13, Property<T14> v14, Property<T15> v15, Property<T16> v16, Property<T17> v17, Property<T18> v18,
			Property<T19> v19, Property<T20> v20) {
		return new _21<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> _22<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> relation(
			String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9,
			Property<T10> v10, Property<T11> v11, Property<T12> v12, Property<T13> v13, Property<T14> v14, Property<T15> v15, Property<T16> v16, Property<T17> v17, Property<T18> v18,
			Property<T19> v19, Property<T20> v20, Property<T21> v21) {
		return new _22<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> _23<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> relation(
			String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9,
			Property<T10> v10, Property<T11> v11, Property<T12> v12, Property<T13> v13, Property<T14> v14, Property<T15> v15, Property<T16> v16, Property<T17> v17, Property<T18> v18,
			Property<T19> v19, Property<T20> v20, Property<T21> v21, Property<T22> v22) {
		return new _23<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22));
	}
	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23> _24<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23> relation(
			String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8, Property<T9> v9,
			Property<T10> v10, Property<T11> v11, Property<T12> v12, Property<T13> v13, Property<T14> v14, Property<T15> v15, Property<T16> v16, Property<T17> v17, Property<T18> v18,
			Property<T19> v19, Property<T20> v20, Property<T21> v21, Property<T22> v22, Property<T23> v23) {
		return new _24<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22, v23));
	}
	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _0 implements Relation {
		String name;
		Tuples._0 properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{};
		}

		public Goal apply(Database db) {
			return RelationN.relation(db, this);
		}

		public Fact fact() {
			return Fact.of(this, Array.of());
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._0 l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept();
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(Functions._1<Database, Goal> goal) {
			Unifiable<Tuples._0> results = LVal.lval(tuple());
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _1<T0> implements Relation {
		String name;
		Tuples._1<Property<T0>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0};
		}

		public Goal apply(Database db, Unifiable<T0> v0) {
			return RelationN.relation(db, this, v0);
		}

		public Fact fact(T0 v0) {
			return Fact.of(this, Array.of(v0));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._1<T0> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(Functions._2<Database, Unifiable<T0>, Goal> goal) {
			Unifiable<Tuples._1<Unifiable<T0>>> results = LVal.lval(tuple(lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _2<T0, T1> implements Relation {
		String name;
		Tuples._2<Property<T0>, Property<T1>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1) {
			return RelationN.relation(db, this, v0, v1);
		}

		public Fact fact(T0 v0, T1 v1) {
			return Fact.of(this, Array.of(v0, v1));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._2<T0, T1> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(Functions._3<Database, Unifiable<T0>, Unifiable<T1>, Goal> goal) {
			Unifiable<Tuples._2<Unifiable<T0>, Unifiable<T1>>> results = LVal.lval(tuple(lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _3<T0, T1, T2> implements Relation {
		String name;
		Tuples._3<Property<T0>, Property<T1>, Property<T2>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2) {
			return RelationN.relation(db, this, v0, v1, v2);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2) {
			return Fact.of(this, Array.of(v0, v1, v2));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._3<T0, T1, T2> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(Functions._4<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Goal> goal) {
			Unifiable<Tuples._3<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>>> results = LVal.lval(tuple(lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _4<T0, T1, T2, T3> implements Relation {
		String name;
		Tuples._4<Property<T0>, Property<T1>, Property<T2>, Property<T3>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3) {
			return RelationN.relation(db, this, v0, v1, v2, v3);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3) {
			return Fact.of(this, Array.of(v0, v1, v2, v3));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._4<T0, T1, T2, T3> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(Functions._5<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Goal> goal) {
			Unifiable<Tuples._4<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>>> results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _5<T0, T1, T2, T3, T4> implements Relation {
		String name;
		Tuples._5<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._5<T0, T1, T2, T3, T4> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(Functions._6<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Goal> goal) {
			Unifiable<Tuples._5<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>>> results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _6<T0, T1, T2, T3, T4, T5> implements Relation {
		String name;
		Tuples._6<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._6<T0, T1, T2, T3, T4, T5> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._7<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Goal> goal) {
			Unifiable<Tuples._6<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>>> results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _7<T0, T1, T2, T3, T4, T5, T6> implements Relation {
		String name;
		Tuples._7<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._7<T0, T1, T2, T3, T4, T5, T6> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._8<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Goal> goal) {
			Unifiable<Tuples._7<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>>> results =
					LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _8<T0, T1, T2, T3, T4, T5, T6, T7> implements Relation {
		String name;
		Tuples._8<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._8<T0, T1, T2, T3, T4, T5, T6, T7> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._9<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Goal> goal) {
			Unifiable<Tuples._8<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>>> results =
					LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _9<T0, T1, T2, T3, T4, T5, T6, T7, T8> implements Relation {
		String name;
		Tuples._9<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7,
				Unifiable<T8> v8) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._9<T0, T1, T2, T3, T4, T5, T6, T7, T8> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._10<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Goal> goal) {
			Unifiable<Tuples._9<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>>> results =
					LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> implements Relation {
		String name;
		Tuples._10<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._11<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Goal> goal) {
			Unifiable<Tuples._10<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>>> results =
					LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> implements Relation {
		String name;
		Tuples._11<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._12<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Goal> goal) {
			Unifiable<Tuples._11<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>>>
					results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> implements Relation {
		String name;
		Tuples._12<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>> properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._13<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Goal> goal) {
			Unifiable<Tuples._12<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>>>
					results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> implements Relation {
		String name;
		Tuples._13<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._14<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Goal> goal) {
			Unifiable<Tuples._13<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>>>
					results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> implements Relation {
		String name;
		Tuples._14<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._15<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Goal> goal) {
			Unifiable<Tuples._14<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>>>
					results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.map(Numbers._13(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> implements Relation {
		String name;
		Tuples._15<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>, Property<T14>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13, properties._14};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13, Unifiable<T14> v14) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13), (T14) f.getFact().getValues().get(14));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._16<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Goal> goal) {
			Unifiable<Tuples._15<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>>>
					results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.map(Numbers._13(), Unifiable::get))
					.map(t -> t.map(Numbers._14(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> implements Relation {
		String name;
		Tuples._16<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>, Property<T14>, Property<T15>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13, properties._14, properties._15};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13, Unifiable<T14> v14, Unifiable<T15> v15) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14, T15 v15) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13), (T14) f.getFact().getValues().get(14), (T15) f.getFact().getValues().get(15));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._17<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Goal> goal) {
			Unifiable<Tuples._16<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>>>
					results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.map(Numbers._13(), Unifiable::get))
					.map(t -> t.map(Numbers._14(), Unifiable::get))
					.map(t -> t.map(Numbers._15(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> implements Relation {
		String name;
		Tuples._17<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>, Property<T14>, Property<T15>, Property<T16>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13, properties._14, properties._15, properties._16};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13, Unifiable<T14> v14, Unifiable<T15> v15, Unifiable<T16> v16) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14, T15 v15, T16 v16) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13), (T14) f.getFact().getValues().get(14), (T15) f.getFact().getValues().get(15),
								(T16) f.getFact().getValues().get(16));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._18<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Goal> goal) {
			Unifiable<Tuples._17<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>>>
					results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.map(Numbers._13(), Unifiable::get))
					.map(t -> t.map(Numbers._14(), Unifiable::get))
					.map(t -> t.map(Numbers._15(), Unifiable::get))
					.map(t -> t.map(Numbers._16(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> implements Relation {
		String name;
		Tuples._18<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>, Property<T14>, Property<T15>, Property<T16>, Property<T17>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13, properties._14, properties._15, properties._16, properties._17};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13, Unifiable<T14> v14, Unifiable<T15> v15, Unifiable<T16> v16, Unifiable<T17> v17) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14, T15 v15, T16 v16, T17 v17) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13), (T14) f.getFact().getValues().get(14), (T15) f.getFact().getValues().get(15),
								(T16) f.getFact().getValues().get(16), (T17) f.getFact().getValues().get(17));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._19<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Goal> goal) {
			Unifiable<Tuples._18<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>>>
					results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.map(Numbers._13(), Unifiable::get))
					.map(t -> t.map(Numbers._14(), Unifiable::get))
					.map(t -> t.map(Numbers._15(), Unifiable::get))
					.map(t -> t.map(Numbers._16(), Unifiable::get))
					.map(t -> t.map(Numbers._17(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> implements Relation {
		String name;
		Tuples._19<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>, Property<T14>, Property<T15>, Property<T16>, Property<T17>, Property<T18>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13, properties._14, properties._15, properties._16, properties._17, properties._18};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13, Unifiable<T14> v14, Unifiable<T15> v15, Unifiable<T16> v16, Unifiable<T17> v17,
				Unifiable<T18> v18) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14, T15 v15, T16 v16, T17 v17, T18 v18) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13), (T14) f.getFact().getValues().get(14), (T15) f.getFact().getValues().get(15),
								(T16) f.getFact().getValues().get(16), (T17) f.getFact().getValues().get(17), (T18) f.getFact().getValues().get(18));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._20<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Unifiable<T18>, Goal> goal) {
			Unifiable<Tuples._19<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Unifiable<T18>>>
					results = LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.map(Numbers._13(), Unifiable::get))
					.map(t -> t.map(Numbers._14(), Unifiable::get))
					.map(t -> t.map(Numbers._15(), Unifiable::get))
					.map(t -> t.map(Numbers._16(), Unifiable::get))
					.map(t -> t.map(Numbers._17(), Unifiable::get))
					.map(t -> t.map(Numbers._18(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _20<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> implements Relation {
		String name;
		Tuples._20<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>, Property<T14>, Property<T15>, Property<T16>, Property<T17>, Property<T18>, Property<T19>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13, properties._14, properties._15, properties._16, properties._17, properties._18, properties._19};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13, Unifiable<T14> v14, Unifiable<T15> v15, Unifiable<T16> v16, Unifiable<T17> v17,
				Unifiable<T18> v18, Unifiable<T19> v19) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14, T15 v15, T16 v16, T17 v17, T18 v18, T19 v19) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._20<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13), (T14) f.getFact().getValues().get(14), (T15) f.getFact().getValues().get(15),
								(T16) f.getFact().getValues().get(16), (T17) f.getFact().getValues().get(17), (T18) f.getFact().getValues().get(18), (T19) f.getFact().getValues().get(19));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._21<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Unifiable<T18>, Unifiable<T19>, Goal> goal) {
			Unifiable<Tuples._20<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Unifiable<T18>, Unifiable<T19>>>
					results =
					LVal.lval(tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.map(Numbers._13(), Unifiable::get))
					.map(t -> t.map(Numbers._14(), Unifiable::get))
					.map(t -> t.map(Numbers._15(), Unifiable::get))
					.map(t -> t.map(Numbers._16(), Unifiable::get))
					.map(t -> t.map(Numbers._17(), Unifiable::get))
					.map(t -> t.map(Numbers._18(), Unifiable::get))
					.map(t -> t.map(Numbers._19(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _21<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> implements Relation {
		String name;
		Tuples._21<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>, Property<T14>, Property<T15>, Property<T16>, Property<T17>, Property<T18>, Property<T19>, Property<T20>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13, properties._14, properties._15, properties._16, properties._17, properties._18, properties._19, properties._20};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13, Unifiable<T14> v14, Unifiable<T15> v15, Unifiable<T16> v16, Unifiable<T17> v17,
				Unifiable<T18> v18, Unifiable<T19> v19, Unifiable<T20> v20) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14, T15 v15, T16 v16, T17 v17, T18 v18, T19 v19, T20 v20) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._21<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13), (T14) f.getFact().getValues().get(14), (T15) f.getFact().getValues().get(15),
								(T16) f.getFact().getValues().get(16), (T17) f.getFact().getValues().get(17), (T18) f.getFact().getValues().get(18), (T19) f.getFact().getValues().get(19),
								(T20) f.getFact().getValues().get(20));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._22<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Unifiable<T18>, Unifiable<T19>, Unifiable<T20>, Goal> goal) {
			Unifiable<Tuples._21<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Unifiable<T18>, Unifiable<T19>, Unifiable<T20>>>
					results = LVal.lval(
					tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.map(Numbers._13(), Unifiable::get))
					.map(t -> t.map(Numbers._14(), Unifiable::get))
					.map(t -> t.map(Numbers._15(), Unifiable::get))
					.map(t -> t.map(Numbers._16(), Unifiable::get))
					.map(t -> t.map(Numbers._17(), Unifiable::get))
					.map(t -> t.map(Numbers._18(), Unifiable::get))
					.map(t -> t.map(Numbers._19(), Unifiable::get))
					.map(t -> t.map(Numbers._20(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _22<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> implements Relation {
		String name;
		Tuples._22<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>, Property<T14>, Property<T15>, Property<T16>, Property<T17>, Property<T18>, Property<T19>, Property<T20>, Property<T21>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13, properties._14, properties._15, properties._16, properties._17, properties._18, properties._19, properties._20,
					properties._21};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13, Unifiable<T14> v14, Unifiable<T15> v15, Unifiable<T16> v16, Unifiable<T17> v17,
				Unifiable<T18> v18, Unifiable<T19> v19, Unifiable<T20> v20, Unifiable<T21> v21) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14, T15 v15, T16 v16, T17 v17, T18 v18, T19 v19, T20 v20,
				T21 v21) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._22<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13), (T14) f.getFact().getValues().get(14), (T15) f.getFact().getValues().get(15),
								(T16) f.getFact().getValues().get(16), (T17) f.getFact().getValues().get(17), (T18) f.getFact().getValues().get(18), (T19) f.getFact().getValues().get(19),
								(T20) f.getFact().getValues().get(20), (T21) f.getFact().getValues().get(21));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._23<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Unifiable<T18>, Unifiable<T19>, Unifiable<T20>, Unifiable<T21>, Goal> goal) {
			Unifiable<Tuples._22<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Unifiable<T18>, Unifiable<T19>, Unifiable<T20>, Unifiable<T21>>>
					results = LVal.lval(
					tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(),
							lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.map(Numbers._13(), Unifiable::get))
					.map(t -> t.map(Numbers._14(), Unifiable::get))
					.map(t -> t.map(Numbers._15(), Unifiable::get))
					.map(t -> t.map(Numbers._16(), Unifiable::get))
					.map(t -> t.map(Numbers._17(), Unifiable::get))
					.map(t -> t.map(Numbers._18(), Unifiable::get))
					.map(t -> t.map(Numbers._19(), Unifiable::get))
					.map(t -> t.map(Numbers._20(), Unifiable::get))
					.map(t -> t.map(Numbers._21(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _23<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> implements Relation {
		String name;
		Tuples._23<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>, Property<T14>, Property<T15>, Property<T16>, Property<T17>, Property<T18>, Property<T19>, Property<T20>, Property<T21>, Property<T22>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13, properties._14, properties._15, properties._16, properties._17, properties._18, properties._19, properties._20,
					properties._21, properties._22};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13, Unifiable<T14> v14, Unifiable<T15> v15, Unifiable<T16> v16, Unifiable<T17> v17,
				Unifiable<T18> v18, Unifiable<T19> v19, Unifiable<T20> v20, Unifiable<T21> v21, Unifiable<T22> v22) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14, T15 v15, T16 v16, T17 v17, T18 v18, T19 v19, T20 v20,
				T21 v21, T22 v22) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._23<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13), (T14) f.getFact().getValues().get(14), (T15) f.getFact().getValues().get(15),
								(T16) f.getFact().getValues().get(16), (T17) f.getFact().getValues().get(17), (T18) f.getFact().getValues().get(18), (T19) f.getFact().getValues().get(19),
								(T20) f.getFact().getValues().get(20), (T21) f.getFact().getValues().get(21), (T22) f.getFact().getValues().get(22));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

		public Functions._1<Database, Validation<Seq<String>, Database>> derived(
				Functions._24<Database, Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Unifiable<T18>, Unifiable<T19>, Unifiable<T20>, Unifiable<T21>, Unifiable<T22>, Goal> goal) {
			Unifiable<Tuples._23<Unifiable<T0>, Unifiable<T1>, Unifiable<T2>, Unifiable<T3>, Unifiable<T4>, Unifiable<T5>, Unifiable<T6>, Unifiable<T7>, Unifiable<T8>, Unifiable<T9>, Unifiable<T10>, Unifiable<T11>, Unifiable<T12>, Unifiable<T13>, Unifiable<T14>, Unifiable<T15>, Unifiable<T16>, Unifiable<T17>, Unifiable<T18>, Unifiable<T19>, Unifiable<T20>, Unifiable<T21>, Unifiable<T22>>>
					results = LVal.lval(
					tuple(lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(), lvar(),
							lvar(), lvar()));
			return db -> db.facts(Goal.runStream(results, results.get().apply(goal.partial(db)))
					.map(Unifiable::get)
					.map(t -> t.map(Numbers._0(), Unifiable::get))
					.map(t -> t.map(Numbers._1(), Unifiable::get))
					.map(t -> t.map(Numbers._2(), Unifiable::get))
					.map(t -> t.map(Numbers._3(), Unifiable::get))
					.map(t -> t.map(Numbers._4(), Unifiable::get))
					.map(t -> t.map(Numbers._5(), Unifiable::get))
					.map(t -> t.map(Numbers._6(), Unifiable::get))
					.map(t -> t.map(Numbers._7(), Unifiable::get))
					.map(t -> t.map(Numbers._8(), Unifiable::get))
					.map(t -> t.map(Numbers._9(), Unifiable::get))
					.map(t -> t.map(Numbers._10(), Unifiable::get))
					.map(t -> t.map(Numbers._11(), Unifiable::get))
					.map(t -> t.map(Numbers._12(), Unifiable::get))
					.map(t -> t.map(Numbers._13(), Unifiable::get))
					.map(t -> t.map(Numbers._14(), Unifiable::get))
					.map(t -> t.map(Numbers._15(), Unifiable::get))
					.map(t -> t.map(Numbers._16(), Unifiable::get))
					.map(t -> t.map(Numbers._17(), Unifiable::get))
					.map(t -> t.map(Numbers._18(), Unifiable::get))
					.map(t -> t.map(Numbers._19(), Unifiable::get))
					.map(t -> t.map(Numbers._20(), Unifiable::get))
					.map(t -> t.map(Numbers._21(), Unifiable::get))
					.map(t -> t.map(Numbers._22(), Unifiable::get))
					.map(t -> t.apply(this::fact))
					.collect(Collectors.toList()));
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class _24<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23> implements Relation {
		String name;
		Tuples._24<Property<T0>, Property<T1>, Property<T2>, Property<T3>, Property<T4>, Property<T5>, Property<T6>, Property<T7>, Property<T8>, Property<T9>, Property<T10>, Property<T11>, Property<T12>, Property<T13>, Property<T14>, Property<T15>, Property<T16>, Property<T17>, Property<T18>, Property<T19>, Property<T20>, Property<T21>, Property<T22>, Property<T23>>
				properties;

		@Override
		public Property<?>[] getArgs() {
			return new Property<?>[]{properties._0, properties._1, properties._2, properties._3, properties._4, properties._5, properties._6, properties._7, properties._8, properties._9,
					properties._10, properties._11, properties._12, properties._13, properties._14, properties._15, properties._16, properties._17, properties._18, properties._19, properties._20,
					properties._21, properties._22, properties._23};
		}

		public Goal apply(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7, Unifiable<T8> v8,
				Unifiable<T9> v9, Unifiable<T10> v10, Unifiable<T11> v11, Unifiable<T12> v12, Unifiable<T13> v13, Unifiable<T14> v14, Unifiable<T15> v15, Unifiable<T16> v16, Unifiable<T17> v17,
				Unifiable<T18> v18, Unifiable<T19> v19, Unifiable<T20> v20, Unifiable<T21> v21, Unifiable<T22> v22, Unifiable<T23> v23) {
			return RelationN.relation(db, this, v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22, v23);
		}

		public Fact fact(T0 v0, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12, T13 v13, T14 v14, T15 v15, T16 v16, T17 v17, T18 v18, T19 v19, T20 v20,
				T21 v21, T22 v22, T23 v23) {
			return Fact.of(this, Array.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22, v23));
		}
		@Override
		public String toString() {
			return name + properties;
		}

		@SuppressWarnings("unchecked")
		public DatabaseEventListener observer(FactChangedEvent event, Consumers._24<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23> l) {
			return e -> e.asFactsChanged()
					.filter(f -> f.getFact().getRelation().equals(this))
					.filter(f -> f.getEvent().equals(event))
					.map(f -> {
						l.accept((T0) f.getFact().getValues().get(0), (T1) f.getFact().getValues().get(1), (T2) f.getFact().getValues().get(2), (T3) f.getFact().getValues().get(3),
								(T4) f.getFact().getValues().get(4), (T5) f.getFact().getValues().get(5), (T6) f.getFact().getValues().get(6), (T7) f.getFact().getValues().get(7),
								(T8) f.getFact().getValues().get(8), (T9) f.getFact().getValues().get(9), (T10) f.getFact().getValues().get(10), (T11) f.getFact().getValues().get(11),
								(T12) f.getFact().getValues().get(12), (T13) f.getFact().getValues().get(13), (T14) f.getFact().getValues().get(14), (T15) f.getFact().getValues().get(15),
								(T16) f.getFact().getValues().get(16), (T17) f.getFact().getValues().get(17), (T18) f.getFact().getValues().get(18), (T19) f.getFact().getValues().get(19),
								(T20) f.getFact().getValues().get(20), (T21) f.getFact().getValues().get(21), (T22) f.getFact().getValues().get(22), (T23) f.getFact().getValues().get(23));
						return observer(event, l);
					})
					.orElseGet(() -> observer(event, l));
		}

	}

}
