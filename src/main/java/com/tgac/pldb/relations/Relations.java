package com.tgac.pldb.relations;

import static com.tgac.functional.Tuples.tuple;

import com.tgac.functional.Functions;
import com.tgac.functional.Tuples;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.Database;
import io.vavr.collection.Array;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Relations {
	public static Relations._0 relation(String name) {
		return new Relations._0(name, tuple());
	}

	public static <T0> Relations._1<T0> relation(String name, Property<T0> v0) {
		return new Relations._1<>(name, tuple(v0));
	}

	public static <T0, T1> Relations._2<T0, T1> relation(String name, Property<T0> v0, Property<T1> v1) {
		return new Relations._2<>(name, tuple(v0, v1));
	}

	public static <T0, T1, T2> Relations._3<T0, T1, T2> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2) {
		return new Relations._3<>(name, tuple(v0, v1, v2));
	}

	public static <T0, T1, T2, T3> Relations._4<T0, T1, T2, T3> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3) {
		return new Relations._4<>(name, tuple(v0, v1, v2, v3));
	}

	public static <T0, T1, T2, T3, T4> Relations._5<T0, T1, T2, T3, T4> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4) {
		return new Relations._5<>(name, tuple(v0, v1, v2, v3, v4));
	}

	public static <T0, T1, T2, T3, T4, T5> Relations._6<T0, T1, T2, T3, T4, T5> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4,
			Property<T5> v5) {
		return new Relations._6<>(name, tuple(v0, v1, v2, v3, v4, v5));
	}

	public static <T0, T1, T2, T3, T4, T5, T6> Relations._7<T0, T1, T2, T3, T4, T5, T6> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3, Property<T4> v4,
			Property<T5> v5, Property<T6> v6) {
		return new Relations._7<>(name, tuple(v0, v1, v2, v3, v4, v5, v6));
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7> Relations._8<T0, T1, T2, T3, T4, T5, T6, T7> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3,
			Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7) {
		return new Relations._8<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7));
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8> Relations._9<T0, T1, T2, T3, T4, T5, T6, T7, T8> relation(String name, Property<T0> v0, Property<T1> v1, Property<T2> v2, Property<T3> v3,
			Property<T4> v4, Property<T5> v5, Property<T6> v6, Property<T7> v7, Property<T8> v8) {
		return new Relations._9<>(name, tuple(v0, v1, v2, v3, v4, v5, v6, v7, v8));
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

		public Goal exists(Database db) {
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
		public <R> Optional<R> exists(Fact fact, Functions._0<R> fn) {
			return Optional.of(fact)
					.filter(f -> f.getRelation().equals(this))
					.map(f -> fn.apply());
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

		public Goal exists(Database db, Unifiable<T0> v0) {
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
		public <R> Optional<R> exists(Fact fact, Functions._1<T0, R> fn) {
			return Optional.of(fact)
					.filter(f -> f.getRelation().equals(this))
					.map(f -> fn.apply((T0) f.getValues().get(0)));
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

		public Goal exists(Database db, Unifiable<T0> v0, Unifiable<T1> v1) {
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
		public <R> Optional<R> exists(Fact fact, Functions._2<T0, T1, R> fn) {
			return Optional.of(fact)
					.filter(f -> f.getRelation().equals(this))
					.map(f -> fn.apply((T0) f.getValues().get(0), (T1) f.getValues().get(1)));
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

		public Goal exists(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2) {
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
		public <R> Optional<R> exists(Fact fact, Functions._3<T0, T1, T2, R> fn) {
			return Optional.of(fact)
					.filter(f -> f.getRelation().equals(this))
					.map(f -> fn.apply((T0) f.getValues().get(0), (T1) f.getValues().get(1), (T2) f.getValues().get(2)));
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

		public Goal exists(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3) {
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
		public <R> Optional<R> exists(Fact fact, Functions._4<T0, T1, T2, T3, R> fn) {
			return Optional.of(fact)
					.filter(f -> f.getRelation().equals(this))
					.map(f -> fn.apply((T0) f.getValues().get(0), (T1) f.getValues().get(1), (T2) f.getValues().get(2), (T3) f.getValues().get(3)));
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

		public Goal exists(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4) {
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
		public <R> Optional<R> exists(Fact fact, Functions._5<T0, T1, T2, T3, T4, R> fn) {
			return Optional.of(fact)
					.filter(f -> f.getRelation().equals(this))
					.map(f -> fn.apply((T0) f.getValues().get(0), (T1) f.getValues().get(1), (T2) f.getValues().get(2), (T3) f.getValues().get(3), (T4) f.getValues().get(4)));
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

		public Goal exists(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5) {
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
		public <R> Optional<R> exists(Fact fact, Functions._6<T0, T1, T2, T3, T4, T5, R> fn) {
			return Optional.of(fact)
					.filter(f -> f.getRelation().equals(this))
					.map(f -> fn.apply((T0) f.getValues().get(0), (T1) f.getValues().get(1), (T2) f.getValues().get(2), (T3) f.getValues().get(3), (T4) f.getValues().get(4),
							(T5) f.getValues().get(5)));
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

		public Goal exists(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6) {
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
		public <R> Optional<R> exists(Fact fact, Functions._7<T0, T1, T2, T3, T4, T5, T6, R> fn) {
			return Optional.of(fact)
					.filter(f -> f.getRelation().equals(this))
					.map(f -> fn.apply((T0) f.getValues().get(0), (T1) f.getValues().get(1), (T2) f.getValues().get(2), (T3) f.getValues().get(3), (T4) f.getValues().get(4), (T5) f.getValues().get(5),
							(T6) f.getValues().get(6)));
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

		public Goal exists(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7) {
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
		public <R> Optional<R> exists(Fact fact, Functions._8<T0, T1, T2, T3, T4, T5, T6, T7, R> fn) {
			return Optional.of(fact)
					.filter(f -> f.getRelation().equals(this))
					.map(f -> fn.apply((T0) f.getValues().get(0), (T1) f.getValues().get(1), (T2) f.getValues().get(2), (T3) f.getValues().get(3), (T4) f.getValues().get(4), (T5) f.getValues().get(5),
							(T6) f.getValues().get(6), (T7) f.getValues().get(7)));
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

		public Goal exists(Database db, Unifiable<T0> v0, Unifiable<T1> v1, Unifiable<T2> v2, Unifiable<T3> v3, Unifiable<T4> v4, Unifiable<T5> v5, Unifiable<T6> v6, Unifiable<T7> v7,
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
		public <R> Optional<R> exists(Fact fact, Functions._9<T0, T1, T2, T3, T4, T5, T6, T7, T8, R> fn) {
			return Optional.of(fact)
					.filter(f -> f.getRelation().equals(this))
					.map(f -> fn.apply((T0) f.getValues().get(0), (T1) f.getValues().get(1), (T2) f.getValues().get(2), (T3) f.getValues().get(3), (T4) f.getValues().get(4), (T5) f.getValues().get(5),
							(T6) f.getValues().get(6), (T7) f.getValues().get(7), (T8) f.getValues().get(8)));
		}
	}

}