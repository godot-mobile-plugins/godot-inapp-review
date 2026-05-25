//
// © 2024-present https://github.com/cengiz-pz
//

package org.godotengine.plugin.inappreview;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.SignalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link InappReviewPlugin}.
 *
 * <h3>Design summary</h3>
 * <ul>
 *   <li><strong>No Mockito spy for signal verification.</strong>
 *       {@code GodotPlugin.emitSignal(String, Object...)} is {@code protected},
 *       so Mockito cannot stub or verify it from a test class that is not itself a
 *       subclass. {@link TestableInappReviewPlugin} solves this by overriding the
 *       method with {@code public} access and recording every call in memory.</li>
 *   <li><strong>Reflection for field injection.</strong>
 *       Private fields ({@code activity}, {@code manager}, {@code reviewInfo}) are
 *       set via {@link #injectField} / read via {@link #readField}, keeping
 *       production visibility unchanged.</li>
 *   <li><strong>Synchronous Task mocks.</strong>
 *       {@link InappReviewPluginFixtures} supplies {@code Task} mocks whose
 *       listeners fire inside {@code addOnSuccessListener} / {@code addOnFailureListener},
 *       so no async machinery is needed in the tests.</li>
 *   <li><strong>{@code isReturnDefaultValues = true}.</strong>
 *       Declared in the Gradle build, this makes un-mocked Android SDK calls
 *       (e.g. {@code Log.e}) return safe defaults rather than throwing.</li>
 *   <li><strong>Reflection for {@code SignalInfo.getParamTypes()}.</strong>
 *       The method is package-private in {@code GodotPlugin}'s package, so
 *       {@link InappReviewPluginFixtures#getParamTypes} uses
 *       {@code setAccessible(true)} to reach it.</li>
 * </ul>
 */
@DisplayName("InappReviewPlugin")
@ExtendWith(MockitoExtension.class)
class InappReviewPluginTest {

	// -------------------------------------------------------------------------
	// Mocks shared across all nested suites
	// -------------------------------------------------------------------------

	@Mock
	private Godot mockGodot;
	@Mock
	private Activity mockActivity;
	@Mock
	private ReviewManager mockReviewManager;
	@Mock
	private ReviewInfo mockReviewInfo;

	/**
	 * The system under test. Using the testable subclass means signal emissions
	 * are captured in memory rather than forwarded to the unavailable Godot engine.
	 */
	private TestableInappReviewPlugin plugin;

	// -------------------------------------------------------------------------
	// Common setup
	// -------------------------------------------------------------------------

	@BeforeEach
	void setUp() throws Exception {
		plugin = new TestableInappReviewPlugin(mockGodot);

		// Inject private dependencies so each test starts from a known state.
		injectField("activity",   mockActivity);
		injectField("manager",    mockReviewManager);
		injectField("reviewInfo", null);   // matches the post-onMainCreate default
	}

	// =========================================================================
	// generate_review_info()
	// =========================================================================

	@Nested
	@DisplayName("generate_review_info()")
	class GenerateReviewInfoTests {

		@Test
		@DisplayName("emits review_info_generated on task success")
		void emitsReviewInfoGeneratedOnSuccess() {
			Task<ReviewInfo> task = InappReviewPluginFixtures.successfulReviewInfoTask(mockReviewInfo);
			when(mockReviewManager.requestReviewFlow()).thenReturn(task);

			plugin.generate_review_info();

			assertTrue(
					plugin.wasSignalEmitted(InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATED),
					"Expected signal '" + InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATED
						+ "' to be emitted");
		}

		@Test
		@DisplayName("emits review_info_generated exactly once on task success")
		void emitsReviewInfoGeneratedExactlyOnce() {
			Task<ReviewInfo> task = InappReviewPluginFixtures.successfulReviewInfoTask(mockReviewInfo);
			when(mockReviewManager.requestReviewFlow()).thenReturn(task);

			plugin.generate_review_info();

			assertEquals(1,
					plugin.emitCount(InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATED));
		}

		@Test
		@DisplayName("stores the ReviewInfo returned by the task so launch_review_flow can use it")
		void storesReviewInfoOnSuccess() throws Exception {
			Task<ReviewInfo> task = InappReviewPluginFixtures.successfulReviewInfoTask(mockReviewInfo);
			when(mockReviewManager.requestReviewFlow()).thenReturn(task);

			plugin.generate_review_info();

			assertSame(mockReviewInfo, readField("reviewInfo"),
					"The 'reviewInfo' field must hold the object delivered by the success listener");
		}

		@Test
		@DisplayName("does not emit the failure signal on task success")
		void doesNotEmitFailureSignalOnSuccess() {
			Task<ReviewInfo> task = InappReviewPluginFixtures.successfulReviewInfoTask(mockReviewInfo);
			when(mockReviewManager.requestReviewFlow()).thenReturn(task);

			plugin.generate_review_info();

			assertFalse(
					plugin.wasSignalEmitted(
							InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATION_FAILED),
							"Failure signal must not be emitted on success");
		}

		@Test
		@DisplayName("emits review_info_generation_failed on task failure")
		void emitsReviewInfoGenerationFailedOnFailure() {
			Task<ReviewInfo> task = InappReviewPluginFixtures.failedReviewInfoTask();
			when(mockReviewManager.requestReviewFlow()).thenReturn(task);

			plugin.generate_review_info();

			assertTrue(
					plugin.wasSignalEmitted(
							InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATION_FAILED),
					"Expected signal '"
							+ InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATION_FAILED
							+ "' to be emitted");
		}

		@Test
		@DisplayName("does not emit the success signal on task failure")
		void doesNotEmitSuccessSignalOnFailure() {
			Task<ReviewInfo> task = InappReviewPluginFixtures.failedReviewInfoTask();
			when(mockReviewManager.requestReviewFlow()).thenReturn(task);

			plugin.generate_review_info();

			assertFalse(
					plugin.wasSignalEmitted(InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATED),
					"Success signal must not be emitted on failure");
		}

		@Test
		@DisplayName("does not throw when Play Core classes are absent at runtime")
		void doesNotThrowOnNoClassDefFoundError() {
			when(mockReviewManager.requestReviewFlow())
					.thenThrow(new NoClassDefFoundError("Simulated missing Play Core class"));

			assertDoesNotThrow(
					() -> plugin.generate_review_info(),
					"NoClassDefFoundError must be caught internally, not propagated");
		}

		@Test
		@DisplayName("does not emit any signal when Play Core classes are absent")
		void doesNotEmitAnySignalOnNoClassDefFoundError() {
			when(mockReviewManager.requestReviewFlow())
					.thenThrow(new NoClassDefFoundError("Simulated missing Play Core class"));

			plugin.generate_review_info();

			assertEquals(0, plugin.totalEmitCount(),
					"No signal must be emitted when a NoClassDefFoundError is caught; "
							+ "got: " + plugin.emittedSignalNames());
		}
	}

	// =========================================================================
	// launch_review_flow()
	// =========================================================================

	@Nested
	@DisplayName("launch_review_flow()")
	class LaunchReviewFlowTests {

		/**
		 * Most launch tests require a non-null ReviewInfo pre-loaded into the
		 * plugin field, simulating a prior successful generate_review_info() call.
		 */
		@BeforeEach
		void preloadReviewInfo() throws Exception {
			injectField("reviewInfo", mockReviewInfo);
		}

		@Test
		@DisplayName("emits review_flow_launched on task success")
		void emitsReviewFlowLaunchedOnSuccess() {
			Task<Void> task = InappReviewPluginFixtures.successfulVoidTask();
			when(mockReviewManager.launchReviewFlow(mockActivity, mockReviewInfo)).thenReturn(task);

			plugin.launch_review_flow();

			assertTrue(
					plugin.wasSignalEmitted(InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCHED),
					"Expected signal '" + InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCHED
							+ "' to be emitted");
		}

		@Test
		@DisplayName("does not emit the failure signal on task success")
		void doesNotEmitFailureSignalOnSuccess() {
			Task<Void> task = InappReviewPluginFixtures.successfulVoidTask();
			when(mockReviewManager.launchReviewFlow(mockActivity, mockReviewInfo)).thenReturn(task);

			plugin.launch_review_flow();

			assertFalse(
					plugin.wasSignalEmitted(
							InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCH_FAILED),
					"Failure signal must not be emitted on success");
		}

		@Test
		@DisplayName("emits review_flow_launch_failed on task failure")
		void emitsReviewFlowLaunchFailedOnFailure() {
			Task<Void> task = InappReviewPluginFixtures.failedVoidTask();
			when(mockReviewManager.launchReviewFlow(mockActivity, mockReviewInfo)).thenReturn(task);

			plugin.launch_review_flow();

			assertTrue(
					plugin.wasSignalEmitted(
							InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCH_FAILED),
					"Expected signal '" + InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCH_FAILED
							+ "' to be emitted");
		}

		@Test
		@DisplayName("does not emit the success signal on task failure")
		void doesNotEmitSuccessSignalOnFailure() {
			Task<Void> task = InappReviewPluginFixtures.failedVoidTask();
			when(mockReviewManager.launchReviewFlow(mockActivity, mockReviewInfo)).thenReturn(task);

			plugin.launch_review_flow();

			assertFalse(
					plugin.wasSignalEmitted(InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCHED),
					"Success signal must not be emitted on task failure");
		}

		@Test
		@DisplayName("emits review_flow_launch_failed immediately when ReviewInfo is null")
		void emitsLaunchFailedWhenReviewInfoIsNull() throws Exception {
			injectField("reviewInfo", null);

			plugin.launch_review_flow();

			assertTrue(
					plugin.wasSignalEmitted(
							InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCH_FAILED),
					"Expected signal '"
							+ InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCH_FAILED
							+ "' when ReviewInfo has not been generated");
		}

		@Test
		@DisplayName("never calls ReviewManager.launchReviewFlow when ReviewInfo is null")
		void neverCallsManagerLaunchWhenReviewInfoIsNull() throws Exception {
			injectField("reviewInfo", null);

			plugin.launch_review_flow();

			verify(mockReviewManager, never()).launchReviewFlow(any(), any());
		}

		@Test
		@DisplayName("does not throw when Play Core classes are absent at runtime")
		void doesNotThrowOnNoClassDefFoundError() {
			when(mockReviewManager.launchReviewFlow(any(), any()))
					.thenThrow(new NoClassDefFoundError("Simulated missing Play Core class"));

			assertDoesNotThrow(
					() -> plugin.launch_review_flow(),
					"NoClassDefFoundError must be caught internally, not propagated");
		}

		@Test
		@DisplayName("does not emit any signal when Play Core classes are absent")
		void doesNotEmitAnySignalOnNoClassDefFoundError() {
			when(mockReviewManager.launchReviewFlow(any(), any()))
					.thenThrow(new NoClassDefFoundError("Simulated missing Play Core class"));

			plugin.launch_review_flow();

			assertEquals(0, plugin.totalEmitCount(),
					"No signal must be emitted when a NoClassDefFoundError is caught; "
							+ "got: " + plugin.emittedSignalNames());
		}
	}

	// =========================================================================
	// get_app_review_url()
	// =========================================================================

	@Nested
	@DisplayName("get_app_review_url()")
	class GetAppReviewUrlTests {

		@Test
		@DisplayName("emits app_review_url_ready with the full Play Store URL")
		void emitsCorrectPlayStoreUrl() {
			InappReviewPluginFixtures.stubActivityPackageName(mockActivity);

			plugin.get_app_review_url();

			TestableInappReviewPlugin.EmittedSignal signal =
					plugin.firstEmission(InappReviewPluginFixtures.SIGNAL_APP_REVIEW_URL_READY);

			assertNotNull(signal, "Signal '" + InappReviewPluginFixtures.SIGNAL_APP_REVIEW_URL_READY
					+ "' was not emitted");
			assertEquals(1, signal.args.length, "Signal must carry exactly one argument");
			assertEquals(InappReviewPluginFixtures.EXPECTED_REVIEW_URL, signal.args[0],
					"Signal argument must be the full Play Store URL");
		}

		@Test
		@DisplayName("URL argument contains the application package name")
		void urlContainsPackageName() {
			InappReviewPluginFixtures.stubActivityPackageName(mockActivity);

			plugin.get_app_review_url();

			TestableInappReviewPlugin.EmittedSignal signal =
					plugin.firstEmission(InappReviewPluginFixtures.SIGNAL_APP_REVIEW_URL_READY);

			assertNotNull(signal, "Signal '" + InappReviewPluginFixtures.SIGNAL_APP_REVIEW_URL_READY
					+ "' was not emitted");
			assertTrue(
					signal.args[0].toString().contains(
							InappReviewPluginFixtures.TEST_PACKAGE_NAME),
					"URL must contain the package name '"
							+ InappReviewPluginFixtures.TEST_PACKAGE_NAME + "'");
		}

		@Test
		@DisplayName("URL argument starts with the Play Store base URL")
		void urlStartsWithPlayStoreBaseUrl() {
			InappReviewPluginFixtures.stubActivityPackageName(mockActivity);

			plugin.get_app_review_url();

			TestableInappReviewPlugin.EmittedSignal signal =
					plugin.firstEmission(InappReviewPluginFixtures.SIGNAL_APP_REVIEW_URL_READY);

			assertNotNull(signal);
			assertTrue(
					signal.args[0].toString()
							.startsWith("https://play.google.com/store/apps/details?id="),
					"URL must start with the Play Store base URL");
		}

		@Test
		@DisplayName("does not emit the failure signal on success")
		void doesNotEmitFailureSignalOnSuccess() {
			InappReviewPluginFixtures.stubActivityPackageName(mockActivity);

			plugin.get_app_review_url();

			assertFalse(
					plugin.wasSignalEmitted(
							InappReviewPluginFixtures.SIGNAL_GET_APP_REVIEW_URL_FAILED),
					"Failure signal must not be emitted on success");
		}

		@Test
		@DisplayName("emits get_app_review_url_failed when context retrieval throws")
		void emitsFailedSignalOnException() {
			when(mockActivity.getApplicationContext())
					.thenThrow(new RuntimeException("Simulated context failure"));

			plugin.get_app_review_url();

			assertTrue(
					plugin.wasSignalEmitted(
							InappReviewPluginFixtures.SIGNAL_GET_APP_REVIEW_URL_FAILED),
					"Expected signal '"
							+ InappReviewPluginFixtures.SIGNAL_GET_APP_REVIEW_URL_FAILED
							+ "' to be emitted on exception");
		}

		@Test
		@DisplayName("does not emit the url-ready signal when context retrieval throws")
		void doesNotEmitUrlReadySignalOnException() {
			when(mockActivity.getApplicationContext())
					.thenThrow(new RuntimeException("Simulated context failure"));

			plugin.get_app_review_url();

			assertFalse(
					plugin.wasSignalEmitted(
							InappReviewPluginFixtures.SIGNAL_APP_REVIEW_URL_READY),
					"URL-ready signal must not be emitted when context retrieval fails");
		}
	}

	// =========================================================================
	// getPluginName()
	// =========================================================================

	@Nested
	@DisplayName("getPluginName()")
	class GetPluginNameTests {

		@Test
		@DisplayName("returns the simple class name 'InappReviewPlugin'")
		void returnsSimpleClassName() {
			assertEquals("InappReviewPlugin", plugin.getPluginName());
		}
	}

	// =========================================================================
	// getPluginSignals()
	// =========================================================================

	@Nested
	@DisplayName("getPluginSignals()")
	class GetPluginSignalsTests {

		private Set<SignalInfo> signals;

		@BeforeEach
		void fetchSignals() {
			signals = plugin.getPluginSignals();
		}

		@Test
		@DisplayName("returns a non-null set")
		void returnsNonNullSet() {
			assertNotNull(signals);
		}

		@Test
		@DisplayName("returns exactly six signals")
		void returnsExactlySixSignals() {
			assertEquals(InappReviewPluginFixtures.EXPECTED_SIGNAL_COUNT, signals.size(),
					"Expected " + InappReviewPluginFixtures.EXPECTED_SIGNAL_COUNT
							+ " signals, got: " + signalNames());
		}

		@Test
		@DisplayName("contains review_info_generated signal")
		void containsReviewInfoGeneratedSignal() {
			assertSignalPresent(InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATED);
		}

		@Test
		@DisplayName("contains review_info_generation_failed signal")
		void containsReviewInfoGenerationFailedSignal() {
			assertSignalPresent(InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATION_FAILED);
		}

		@Test
		@DisplayName("contains review_flow_launched signal")
		void containsReviewFlowLaunchedSignal() {
			assertSignalPresent(InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCHED);
		}

		@Test
		@DisplayName("contains review_flow_launch_failed signal")
		void containsReviewFlowLaunchFailedSignal() {
			assertSignalPresent(InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCH_FAILED);
		}

		@Test
		@DisplayName("contains app_review_url_ready signal")
		void containsAppReviewUrlReadySignal() {
			assertSignalPresent(InappReviewPluginFixtures.SIGNAL_APP_REVIEW_URL_READY);
		}

		@Test
		@DisplayName("contains get_app_review_url_failed signal")
		void containsGetAppReviewUrlFailedSignal() {
			assertSignalPresent(InappReviewPluginFixtures.SIGNAL_GET_APP_REVIEW_URL_FAILED);
		}

		@Test
		@DisplayName("app_review_url_ready carries exactly one String parameter")
		void appReviewUrlReadySignalHasOneStringParameter() throws Exception {
			SignalInfo urlSignal = findSignal(InappReviewPluginFixtures.SIGNAL_APP_REVIEW_URL_READY);
			Class<?>[] paramTypes = InappReviewPluginFixtures.getParamTypes(urlSignal);

			assertEquals(1, paramTypes.length,
					"Signal '" + InappReviewPluginFixtures.SIGNAL_APP_REVIEW_URL_READY
							+ "' must declare exactly one parameter");
			assertEquals(String.class, paramTypes[0],
					"Signal '" + InappReviewPluginFixtures.SIGNAL_APP_REVIEW_URL_READY
							+ "' parameter must be of type String");
		}

		@Test
		@DisplayName("all no-payload signals carry zero parameters")
		void noPayloadSignalsHaveZeroParameters() throws Exception {
			Set<String> noArgSignals = Set.of(
					InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATED,
					InappReviewPluginFixtures.SIGNAL_REVIEW_INFO_GENERATION_FAILED,
					InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCHED,
					InappReviewPluginFixtures.SIGNAL_REVIEW_FLOW_LAUNCH_FAILED,
					InappReviewPluginFixtures.SIGNAL_GET_APP_REVIEW_URL_FAILED
			);

			for (SignalInfo signal : signals) {
				if (noArgSignals.contains(signal.getName())) {
					Class<?>[] paramTypes = InappReviewPluginFixtures.getParamTypes(signal);
					assertEquals(0, paramTypes.length,
							"Signal '" + signal.getName() + "' must declare no parameters");
				}
			}
		}

		// -- Helpers -----------------------------------------------------------

		private void assertSignalPresent(String signalName) {
			assertTrue(signalNames().contains(signalName),
					"Expected signal '" + signalName + "' not found in: " + signalNames());
		}

		private SignalInfo findSignal(String signalName) {
			return signals.stream()
				.filter(s -> s.getName().equals(signalName))
				.findFirst()
				.orElseThrow(() ->
					new AssertionError(
							"Signal '" + signalName + "' not found in: " + signalNames()));
		}

		private Set<String> signalNames() {
			return signals.stream()
				.map(SignalInfo::getName)
				.collect(Collectors.toSet());
		}
	}

	// =========================================================================
	// onMainCreate()
	// =========================================================================

	@Nested
	@DisplayName("onMainCreate()")
	class OnMainCreateTests {

		// ReviewManagerFactory.create(activity) is a static Play Core call that
		// internally reaches android.content.pm.PackageManager at runtime.
		// Under the JVM unit-test environment PackageManager is an Android stub
		// whose methods return null, causing an NPE deep inside the Play Core
		// library — well beyond the code under test.
		//
		// Mockito 5's built-in mockStatic intercepts the factory call and
		// returns the already-mocked ReviewManager, keeping the test hermetic
		// and focused on what onMainCreate actually owns: storing the Activity
		// reference and resetting the reviewInfo field.

		@Test
		@DisplayName("stores the supplied Activity in the 'activity' field")
		void storesSuppliedActivity() throws Exception {
			try (MockedStatic<ReviewManagerFactory> factory =
					mockStatic(ReviewManagerFactory.class)) {
				factory.when(() -> ReviewManagerFactory.create(mockActivity))
						.thenReturn(mockReviewManager);

				plugin.onMainCreate(mockActivity);

				assertSame(mockActivity, readField("activity"), "onMainCreate must store the supplied Activity");
			}
		}

		@Test
		@DisplayName("initialises the ReviewManager via ReviewManagerFactory")
		void initialisesReviewManager() throws Exception {
			try (MockedStatic<ReviewManagerFactory> factory =
					mockStatic(ReviewManagerFactory.class)) {
				factory.when(() -> ReviewManagerFactory.create(mockActivity))
						.thenReturn(mockReviewManager);

				plugin.onMainCreate(mockActivity);

				assertSame(mockReviewManager, readField("manager"),
						"onMainCreate must store the ReviewManager returned by the factory");
			}
		}

		@Test
		@DisplayName("resets reviewInfo to null on each create")
		void resetsReviewInfoToNull() throws Exception {
			// Pre-load a stale ReviewInfo to confirm it gets cleared.
			injectField("reviewInfo", mockReviewInfo);

			try (MockedStatic<ReviewManagerFactory> factory =
					mockStatic(ReviewManagerFactory.class)) {
				factory.when(() -> ReviewManagerFactory.create(mockActivity))
						.thenReturn(mockReviewManager);

				plugin.onMainCreate(mockActivity);

				assertNull(readField("reviewInfo"), "onMainCreate must reset reviewInfo to null");
			}
		}

		@Test
		@DisplayName("does not throw during normal initialisation")
		void doesNotThrowDuringInitialisation() {
			try (MockedStatic<ReviewManagerFactory> factory =
					mockStatic(ReviewManagerFactory.class)) {
				factory.when(() -> ReviewManagerFactory.create(mockActivity)).thenReturn(mockReviewManager);

				assertDoesNotThrow(() -> plugin.onMainCreate(mockActivity));
			}
		}
	}

	// =========================================================================
	// Private test helpers
	// =========================================================================

	/**
	 * Sets a private field on the plugin instance by name.
	 *
	 * @param name  simple field name declared in {@link InappReviewPlugin}
	 * @param value value to inject (may be {@code null})
	 */
	private void injectField(String name, Object value) throws Exception {
		Field field = InappReviewPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(plugin, value);
	}

	/**
	 * Reads a private field from the plugin instance by name.
	 *
	 * @param name simple field name declared in {@link InappReviewPlugin}
	 * @return the current field value (may be {@code null})
	 */
	private Object readField(String name) throws Exception {
		Field field = InappReviewPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(plugin);
	}
}
