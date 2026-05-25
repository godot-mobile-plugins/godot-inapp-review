//
// © 2024-present https://github.com/cengiz-pz
//

package org.godotengine.plugin.inappreview;

import org.godotengine.godot.Godot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test-only subclass of {@link InappReviewPlugin} used exclusively in unit tests.
 *
 * <h3>Why a subclass instead of a Mockito spy?</h3>
 * {@code GodotPlugin.emitSignal(String, Object...)} is {@code protected}, making
 * it inaccessible from test code that is not itself a subclass of
 * {@code GodotPlugin}. Mockito therefore cannot stub or verify it via a spy held
 * in a field of an unrelated test class. Subclassing and overriding the method
 * with {@code public} visibility — which the Java spec (JLS §8.4.8.3) explicitly
 * permits — removes that constraint entirely and keeps the tests free of
 * reflection-based workarounds for signal assertions.
 *
 * <h3>What this class does</h3>
 * <ul>
 *   <li>Overrides {@link #emitSignal(String, Object...)} to record every call in
 *       an in-memory list rather than forwarding to the Godot engine (which is
 *       unavailable in the JVM unit-test environment).</li>
 *   <li>Exposes query helpers ({@link #wasSignalEmitted}, {@link #emitCount},
 *       {@link #firstEmission}, …) so test assertions read naturally.</li>
 *   <li>Provides {@link #clearSignals()} for nested test classes that need a
 *       clean slate between individual test methods.</li>
 * </ul>
 *
 * <p>No production code must ever reference this class.
 */
public final class TestableInappReviewPlugin extends InappReviewPlugin {

	// -------------------------------------------------------------------------
	// EmittedSignal record
	// -------------------------------------------------------------------------

	/**
	 * Immutable snapshot of a single {@link #emitSignal} call.
	 */
	public static final class EmittedSignal {

		/** The signal name passed to {@code emitSignal}. */
		public final String name;

		/** A defensive copy of the varargs array (never {@code null}). */
		public final Object[] args;

		EmittedSignal(String name, Object[] args) {
			this.name = name;
			this.args = (args != null) ? args.clone() : new Object[0];
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("EmittedSignal{name='").append(name).append('\'');
			if (args.length > 0) {
				sb.append(", args=[");
				for (int i = 0; i < args.length; i++) {
					if (i > 0) {
						sb.append(", ");
					}
					sb.append(args[i]);
				}
				sb.append(']');
			}
			return sb.append('}').toString();
		}
	}

	// -------------------------------------------------------------------------
	// State
	// -------------------------------------------------------------------------

	private final List<EmittedSignal> emittedSignals = new ArrayList<>();

	// -------------------------------------------------------------------------
	// Constructor
	// -------------------------------------------------------------------------

	public TestableInappReviewPlugin(Godot godot) {
		super(godot);
	}

	// -------------------------------------------------------------------------
	// emitSignal override — core of this class
	// -------------------------------------------------------------------------

	/**
	 * Records the signal call instead of forwarding to the Godot engine.
	 *
	 * <p>Visibility is widened from {@code protected} (in {@code GodotPlugin}) to
	 * {@code public} so that test code can call or mock this method if ever needed
	 * without subclassing again.
	 */
	@Override
	public void emitSignal(String signalName, Object... signalArgs) {
		emittedSignals.add(new EmittedSignal(signalName, signalArgs));
	}

	// -------------------------------------------------------------------------
	// Query helpers used by test assertions
	// -------------------------------------------------------------------------

	/**
	 * Returns an unmodifiable view of every signal emission in call order.
	 */
	public List<EmittedSignal> getEmittedSignals() {
		return Collections.unmodifiableList(emittedSignals);
	}

	/**
	 * Returns {@code true} if {@code emitSignal} was called at least once with
	 * the given signal name.
	 */
	public boolean wasSignalEmitted(String signalName) {
		return emittedSignals.stream().anyMatch(s -> s.name.equals(signalName));
	}

	/**
	 * Returns how many times {@code emitSignal} was called with the given name.
	 */
	public int emitCount(String signalName) {
		return (int) emittedSignals.stream()
				.filter(s -> s.name.equals(signalName))
				.count();
	}

	/**
	 * Returns the first emission with the given name, or {@code null} if none.
	 */
	public EmittedSignal firstEmission(String signalName) {
		return emittedSignals.stream()
				.filter(s -> s.name.equals(signalName))
				.findFirst()
				.orElse(null);
	}

	/**
	 * Returns the total number of {@code emitSignal} calls across all signal names.
	 */
	public int totalEmitCount() {
		return emittedSignals.size();
	}

	/**
	 * Returns the distinct signal names that have been emitted, in emission order
	 * with duplicates removed.
	 */
	public List<String> emittedSignalNames() {
		return emittedSignals.stream()
				.map(s -> s.name)
				.distinct()
				.collect(Collectors.toList());
	}

	/**
	 * Clears all recorded emissions. Useful when a {@code @BeforeEach} already
	 * constructs the plugin but a specific nested class needs a clean baseline.
	 */
	public void clearSignals() {
		emittedSignals.clear();
	}
}
