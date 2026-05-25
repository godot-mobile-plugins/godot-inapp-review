//
// © 2024-present https://github.com/cengiz-pz
//

package org.godotengine.plugin.inappreview;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;

import org.godotengine.godot.plugin.SignalInfo;

import java.lang.reflect.Method;

/**
 * Shared test fixtures for {@link InappReviewPluginTest}.
 *
 * <p>Provides:
 * <ul>
 *   <li>Signal-name constants that mirror the private constants inside
 *       {@link InappReviewPlugin}, keeping assertions readable without
 *       requiring reflection to read constant values.</li>
 *   <li>Factory methods that build pre-configured {@code Task} mocks whose
 *       listeners fire synchronously, eliminating async complexity in tests.</li>
 *   <li>A stub helper that wires
 *       {@code Activity.getApplicationContext().getPackageName()} to a fixed
 *       package name so URL-generation assertions are deterministic.</li>
 *   <li>A reflection helper for {@code SignalInfo.getParamTypes()}, which is
 *       package-private and therefore inaccessible from test code directly.</li>
 * </ul>
 */
public final class InappReviewPluginFixtures {

	// -------------------------------------------------------------------------
	// Signal-name constants (mirrors private constants in InappReviewPlugin)
	// -------------------------------------------------------------------------

	public static final String SIGNAL_REVIEW_INFO_GENERATED         = "review_info_generated";
	public static final String SIGNAL_REVIEW_INFO_GENERATION_FAILED = "review_info_generation_failed";
	public static final String SIGNAL_REVIEW_FLOW_LAUNCHED          = "review_flow_launched";
	public static final String SIGNAL_REVIEW_FLOW_LAUNCH_FAILED     = "review_flow_launch_failed";
	public static final String SIGNAL_APP_REVIEW_URL_READY          = "app_review_url_ready";
	public static final String SIGNAL_GET_APP_REVIEW_URL_FAILED     = "get_app_review_url_failed";

	// -------------------------------------------------------------------------
	// Test data
	// -------------------------------------------------------------------------

	/** Package name returned by the mocked {@link Context}. */
	public static final String TEST_PACKAGE_NAME = "org.godotengine.plugin.inappreview.demo";

	/** Full Play Store URL the plugin is expected to emit. */
	public static final String EXPECTED_REVIEW_URL =
			"https://play.google.com/store/apps/details?id=" + TEST_PACKAGE_NAME;

	/** Total number of signals the plugin must register. */
	public static final int EXPECTED_SIGNAL_COUNT = 6;

	// Prevent instantiation — this is a utility class.
	private InappReviewPluginFixtures() {
	}

	// -------------------------------------------------------------------------
	// Activity / Context helpers
	// -------------------------------------------------------------------------

	/**
	 * Stubs {@code mockActivity.getApplicationContext().getPackageName()} so
	 * that it returns {@link #TEST_PACKAGE_NAME}.
	 *
	 * @param mockActivity a Mockito mock of {@link Activity}
	 */
	public static void stubActivityPackageName(Activity mockActivity) {
		Context mockContext = mock(Context.class);
		when(mockActivity.getApplicationContext()).thenReturn(mockContext);
		when(mockContext.getPackageName()).thenReturn(TEST_PACKAGE_NAME);
	}

	// -------------------------------------------------------------------------
	// Task<ReviewInfo> factory methods
	// -------------------------------------------------------------------------

	/**
	 * Returns a {@code Task<ReviewInfo>} mock that immediately invokes its
	 * {@code OnSuccessListener} with {@code reviewInfo} as soon as
	 * {@code addOnSuccessListener} is called.
	 *
	 * @param reviewInfo the {@link ReviewInfo} object to deliver
	 * @return a synchronous success task
	 */
	@SuppressWarnings("unchecked")
	public static Task<ReviewInfo> successfulReviewInfoTask(ReviewInfo reviewInfo) {
		Task<ReviewInfo> task = (Task<ReviewInfo>) mock(Task.class);

		when(task.addOnSuccessListener(any())).thenAnswer(invocation -> {
			OnSuccessListener<ReviewInfo> listener = invocation.getArgument(0);
			listener.onSuccess(reviewInfo);
			return task;
		});
		when(task.addOnFailureListener(any())).thenReturn(task);

		return task;
	}

	/**
	 * Returns a {@code Task<ReviewInfo>} mock that immediately invokes its
	 * {@code OnFailureListener} with a generic exception as soon as
	 * {@code addOnFailureListener} is called.
	 *
	 * @return a synchronous failure task
	 */
	@SuppressWarnings("unchecked")
	public static Task<ReviewInfo> failedReviewInfoTask() {
		Task<ReviewInfo> task = (Task<ReviewInfo>) mock(Task.class);

		when(task.addOnSuccessListener(any())).thenReturn(task);
		when(task.addOnFailureListener(any())).thenAnswer(invocation -> {
			OnFailureListener listener = invocation.getArgument(0);
			listener.onFailure(new Exception("Simulated review-info request failure"));
			return task;
		});

		return task;
	}

	// -------------------------------------------------------------------------
	// Task<Void> factory methods
	// -------------------------------------------------------------------------

	/**
	 * Returns a {@code Task<Void>} mock that immediately invokes its
	 * {@code OnSuccessListener} with {@code null} as soon as
	 * {@code addOnSuccessListener} is called.
	 *
	 * @return a synchronous success task
	 */
	@SuppressWarnings("unchecked")
	public static Task<Void> successfulVoidTask() {
		Task<Void> task = (Task<Void>) mock(Task.class);

		when(task.addOnSuccessListener(any())).thenAnswer(invocation -> {
			OnSuccessListener<Void> listener = invocation.getArgument(0);
			listener.onSuccess(null);
			return task;
		});
		when(task.addOnFailureListener(any())).thenReturn(task);

		return task;
	}

	/**
	 * Returns a {@code Task<Void>} mock that immediately invokes its
	 * {@code OnFailureListener} with a generic exception as soon as
	 * {@code addOnFailureListener} is called.
	 *
	 * @return a synchronous failure task
	 */
	@SuppressWarnings("unchecked")
	public static Task<Void> failedVoidTask() {
		Task<Void> task = (Task<Void>) mock(Task.class);

		when(task.addOnSuccessListener(any())).thenReturn(task);
		when(task.addOnFailureListener(any())).thenAnswer(invocation -> {
			OnFailureListener listener = invocation.getArgument(0);
			listener.onFailure(new Exception("Simulated review-flow launch failure"));
			return task;
		});

		return task;
	}

	// -------------------------------------------------------------------------
	// SignalInfo reflection helper
	// -------------------------------------------------------------------------

	/**
	 * Returns the parameter types declared on a {@link SignalInfo} via reflection.
	 *
	 * <p>{@code SignalInfo.getParamTypes()} has package-private visibility and is
	 * therefore inaccessible from this test package directly. This helper uses
	 * {@link Method#setAccessible(boolean)} to bypass that restriction so that
	 * signal-parameter assertions remain possible without forking the Godot source.
	 *
	 * @param signal the {@link SignalInfo} to inspect
	 * @return the declared parameter types (may be empty, never {@code null})
	 * @throws ReflectiveOperationException if the method cannot be found or invoked
	 */
	public static Class<?>[] getParamTypes(SignalInfo signal) throws ReflectiveOperationException {
		Method method = SignalInfo.class.getDeclaredMethod("getParamTypes");
		method.setAccessible(true);
		Class<?>[] result = (Class<?>[]) method.invoke(signal);
		return (result != null) ? result : new Class<?>[0];
	}
}
