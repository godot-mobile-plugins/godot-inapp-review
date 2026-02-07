//
// © 2024-present https://github.com/cengiz-pz
//

#import <Foundation/Foundation.h>

#include "core/config/project_settings.h"

#import "inapp_review_plugin.h"
#import "inapp_review_plugin-Swift.h"


String const REVIEW_INFO_GENERATED_SIGNAL = "review_info_generated";
String const REVIEW_INFO_GENERATION_FAILED_SIGNAL = "review_info_generation_failed";
String const REVIEW_FLOW_LAUNCHED_SIGNAL = "review_flow_launched";
String const REVIEW_FLOW_LAUNCH_FAILED_SIGNAL = "review_flow_launch_failed";
String const APP_REVIEW_URL_READY_SIGNAL = "app_review_url_ready";
String const GET_APP_REVIEW_URL_FAILED_SIGNAL = "get_app_review_url_failed";

static NSString *const kLogTag = @"InappReviewPlugin::";


void InappReviewPlugin::_bind_methods() {
	ClassDB::bind_method(D_METHOD("generate_review_info"), &InappReviewPlugin::generate_review_info);
	ClassDB::bind_method(D_METHOD("launch_review_flow"), &InappReviewPlugin::launch_review_flow);
	ClassDB::bind_method(D_METHOD("get_app_review_url"), &InappReviewPlugin::get_app_review_url);

	ADD_SIGNAL(MethodInfo(REVIEW_INFO_GENERATED_SIGNAL));
	ADD_SIGNAL(MethodInfo(REVIEW_INFO_GENERATION_FAILED_SIGNAL));
	ADD_SIGNAL(MethodInfo(REVIEW_FLOW_LAUNCHED_SIGNAL));
	ADD_SIGNAL(MethodInfo(REVIEW_FLOW_LAUNCH_FAILED_SIGNAL));
	ADD_SIGNAL(MethodInfo(APP_REVIEW_URL_READY_SIGNAL, PropertyInfo(Variant::STRING, "url")));
	ADD_SIGNAL(MethodInfo(GET_APP_REVIEW_URL_FAILED_SIGNAL));
}

// Only for platform parity.
void InappReviewPlugin::generate_review_info() {
	NSLog(@"%@ generate_review_info", kLogTag);

	emit_signal(REVIEW_INFO_GENERATED_SIGNAL);
}

void InappReviewPlugin::launch_review_flow() {
	NSLog(@"%@ launch_review_flow", kLogTag);
	[InappReview requestReview];

	emit_signal(REVIEW_FLOW_LAUNCHED_SIGNAL);
}

void InappReviewPlugin::get_app_review_url() {
	NSLog(@"%@ get_app_review_url", kLogTag);

	InappReviewPlugin *plugin_instance = this;

	InappReview *swiftInstance = [[InappReview alloc] init];
	[swiftInstance getAppReviewUrlWithCompletion:^(NSURL * _Nullable url) {
		if (url) {
			NSLog(@"%@ App review URL successfully constructed: %@", kLogTag, url);
			plugin_instance->call_deferred(
				"emit_signal",
				REVIEW_INFO_GENERATED_SIGNAL,
				String(url.absoluteString.UTF8String)
			);
		} else {
			NSLog(@"%@ Error: Failed to construct app review URL", kLogTag);
			plugin_instance->call_deferred("emit_signal", GET_APP_REVIEW_URL_FAILED_SIGNAL);
		}
	}];
}

InappReviewPlugin::InappReviewPlugin() {
	NSLog(@"%@ constructor", kLogTag);
}

InappReviewPlugin::~InappReviewPlugin() {
	NSLog(@"%@ destructor", kLogTag);
}
