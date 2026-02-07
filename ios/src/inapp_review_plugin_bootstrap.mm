//
// © 2024-present https://github.com/cengiz-pz
//
#import <Foundation/Foundation.h>

#import "inapp_review_plugin_bootstrap.h"

#import "inapp_review_plugin.h"
#import "core/config/engine.h"

InappReviewPlugin *inapp_review_plugin;

void inapp_review_plugin_init() {
	NSLog(@"InappReviewPlugin: Initializing plugin at timestamp: %f", [[NSDate date] timeIntervalSince1970]);
	inapp_review_plugin = memnew(InappReviewPlugin);
	Engine::get_singleton()->add_singleton(Engine::Singleton("InappReviewPlugin", inapp_review_plugin));
	NSLog(@"InappReviewPlugin: Singleton registered");
}

void inapp_review_plugin_deinit() {
	NSLog(@"InappReviewPlugin: Deinitializing plugin");
	if (inapp_review_plugin) {
		memdelete(inapp_review_plugin);
		inapp_review_plugin = nullptr;
	}
}
