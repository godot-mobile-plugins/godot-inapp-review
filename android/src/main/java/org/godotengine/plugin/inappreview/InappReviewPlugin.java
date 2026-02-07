//
// © 2024-present https://github.com/cengiz-pz
//

package org.godotengine.plugin.inappreview;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

/**
 * Plugin that allows in-app reviews for the Google Play store.
 */
public class InappReviewPlugin extends GodotPlugin {
	private static final String CLASS_NAME = InappReviewPlugin.class.getSimpleName();
	private static final String LOG_TAG = "godot::" + CLASS_NAME;

	private static final String SIGNAL_NAME_REVIEW_INFO_GENERATED = "review_info_generated";
	private static final String SIGNAL_NAME_REVIEW_INFO_GENERATION_FAILED = "review_info_generation_failed";
	private static final String SIGNAL_NAME_REVIEW_FLOW_LAUNCHED = "review_flow_launched";
	private static final String SIGNAL_NAME_REVIEW_FLOW_LAUNCH_FAILED = "review_flow_launch_failed";
	private static final String SIGNAL_NAME_APP_REVIEW_URL_READY = "app_review_url_ready";
	private static final String SIGNAL_NAME_GET_APP_REVIEW_URL_FAILED = "get_app_review_url_failed";

	private Activity activity;
	private ReviewManager manager;
	private ReviewInfo reviewInfo;	// needed to launch review flow

	public InappReviewPlugin(Godot godot) {
		super(godot);
	}

	/**
	 * Request the generation of a ReviewInfo object. This object is needed before review flow
	 * can be launched
	 */
	@UsedByGodot
	public void generate_review_info() {
		try {
			Task<ReviewInfo> request = manager.requestReviewFlow();
			request.addOnSuccessListener(result -> {
				reviewInfo = result; // the result of the task is a ReviewInfo object
				emitSignal(SIGNAL_NAME_REVIEW_INFO_GENERATED);
			});
			request.addOnFailureListener(task -> emitSignal(SIGNAL_NAME_REVIEW_INFO_GENERATION_FAILED));
		}
		catch (java.lang.NoClassDefFoundError error) {
			StringWriter sw = new StringWriter();
			error.printStackTrace(new PrintWriter(sw));
			Log.e(LOG_TAG, String.format("%s():: unable to generate review flow due to %s",
					"generate_review_info", sw));
		}
	}

	/**
	 * Launches and displays the review flow to the user. Requires a prior successful call to
	 * {@link #generate_review_info()} method
	 */
	@UsedByGodot
	public void launch_review_flow() {
		try {
			if (reviewInfo != null) {
				Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
				flow.addOnSuccessListener(task -> emitSignal(SIGNAL_NAME_REVIEW_FLOW_LAUNCHED));
				flow.addOnFailureListener(task -> emitSignal(SIGNAL_NAME_REVIEW_FLOW_LAUNCH_FAILED));
			} else {
				emitSignal(SIGNAL_NAME_REVIEW_FLOW_LAUNCH_FAILED);
				Log.e(LOG_TAG, String.format("%s():: unable to launch review flow because ReviewInfo has not been generated",
						"launch_review_flow"));
			}
		}
		catch (java.lang.NoClassDefFoundError error) {
			StringWriter sw = new StringWriter();
			error.printStackTrace(new PrintWriter(sw));
			Log.e(LOG_TAG, String.format("%s():: unable to launch review flow due to %s",
					"launch_review_flow", sw));
		}
	}

	@UsedByGodot
	public void get_app_review_url() {
		try {
			String packageName = activity.getApplicationContext().getPackageName();
			emitSignal(SIGNAL_NAME_APP_REVIEW_URL_READY, "https://play.google.com/store/apps/details?id=" + packageName);
		} catch (Exception e) {
			Log.e(LOG_TAG, "get_app_review_url() failed due to " + e.getMessage());
			emitSignal(SIGNAL_NAME_GET_APP_REVIEW_URL_FAILED);
		}
	}

	@NonNull
	@Override
	public String getPluginName() {
		return CLASS_NAME;
	}

	@NonNull
	@Override
	public Set<SignalInfo> getPluginSignals() {
		Set<SignalInfo> signals = new ArraySet<>();

		signals.add(new SignalInfo(SIGNAL_NAME_REVIEW_INFO_GENERATED));
		signals.add(new SignalInfo(SIGNAL_NAME_REVIEW_INFO_GENERATION_FAILED));
		signals.add(new SignalInfo(SIGNAL_NAME_REVIEW_FLOW_LAUNCHED));
		signals.add(new SignalInfo(SIGNAL_NAME_REVIEW_FLOW_LAUNCH_FAILED));
		signals.add(new SignalInfo(SIGNAL_NAME_APP_REVIEW_URL_READY, String.class));
		signals.add(new SignalInfo(SIGNAL_NAME_GET_APP_REVIEW_URL_FAILED));

		return signals;
	}

	@Nullable
	@Override
	public View onMainCreate(Activity activity) {
		this.activity = activity;
		manager = ReviewManagerFactory.create(activity);
		reviewInfo = null;
		return super.onMainCreate(activity);
	}
}
