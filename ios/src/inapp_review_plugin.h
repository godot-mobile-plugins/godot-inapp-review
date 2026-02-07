//
// © 2024-present https://github.com/cengiz-pz
//
#pragma once

#include "core/object/object.h"
#include "core/object/class_db.h"

extern String const REVIEW_INFO_GENERATED_SIGNAL;
extern String const REVIEW_INFO_GENERATION_FAILED_SIGNAL;
extern String const REVIEW_FLOW_LAUNCHED_SIGNAL;
extern String const REVIEW_FLOW_LAUNCH_FAILED_SIGNAL;


class InappReviewPlugin : public Object {
	GDCLASS(InappReviewPlugin, Object);

	static void _bind_methods();

public:
	void generate_review_info();
	void launch_review_flow();
	void get_app_review_url();

	InappReviewPlugin();
	~InappReviewPlugin();
};
