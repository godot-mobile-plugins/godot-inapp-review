#
# © 2024-present https://github.com/cengiz-pz
#

@tool
@icon("icon.png")
class_name InappReview extends Node

const PLUGIN_SINGLETON_NAME: String = "@pluginName@"

signal review_info_generated()
signal review_info_generation_failed()
signal review_flow_launched()
signal review_flow_launch_failed()
signal app_review_url_ready(url: String)
signal get_app_review_url_failed()

const SIGNAL_NAME_REVIEW_INFO_GENERATED: String = "review_info_generated";
const SIGNAL_NAME_REVIEW_INFO_GENERATION_FAILED: String = "review_info_generation_failed";
const SIGNAL_NAME_REVIEW_FLOW_LAUNCHED: String = "review_flow_launched";
const SIGNAL_NAME_REVIEW_FLOW_LAUNCH_FAILED: String = "review_flow_launch_failed";
const SIGNAL_NAME_APP_REVIEW_URL_READY: String = "app_review_url_ready";
const SIGNAL_NAME_GET_APP_REVIEW_URL_FAILED: String = "get_app_review_url_failed";

var _plugin_singleton: Object


func _ready() -> void:
	_update_plugin()


func _notification(a_what: int) -> void:
	if a_what == NOTIFICATION_APPLICATION_RESUMED:
		_update_plugin()


func _update_plugin() -> void:
	if _plugin_singleton == null:
		if Engine.has_singleton(PLUGIN_SINGLETON_NAME):
			_plugin_singleton = Engine.get_singleton(PLUGIN_SINGLETON_NAME)
			_connect_signals()
		elif not OS.has_feature("editor_hint"):
			log_error("%s singleton not found!" % PLUGIN_SINGLETON_NAME)


func _connect_signals() -> void:
	_plugin_singleton.connect(SIGNAL_NAME_REVIEW_INFO_GENERATED, _on_review_info_generated)
	_plugin_singleton.connect(SIGNAL_NAME_REVIEW_INFO_GENERATION_FAILED, _on_review_info_generation_failed)
	_plugin_singleton.connect(SIGNAL_NAME_REVIEW_FLOW_LAUNCHED, _on_review_flow_launched)
	_plugin_singleton.connect(SIGNAL_NAME_REVIEW_FLOW_LAUNCH_FAILED, _on_review_flow_launch_failed)
	_plugin_singleton.connect(SIGNAL_NAME_APP_REVIEW_URL_READY, _on_app_review_url_ready)
	_plugin_singleton.connect(SIGNAL_NAME_GET_APP_REVIEW_URL_FAILED, _on_get_app_review_url_failed)


func generate_review_info() -> void:
	if _plugin_singleton != null:
		_plugin_singleton.generate_review_info()
	else:
		log_error("%s plugin not initialized" % PLUGIN_SINGLETON_NAME)


func launch_review_flow() -> void:
	if _plugin_singleton != null:
		_plugin_singleton.launch_review_flow()
	else:
		log_error("%s plugin not initialized" % PLUGIN_SINGLETON_NAME)


func get_app_review_url() -> void:
	if _plugin_singleton != null:
		_plugin_singleton.get_app_review_url()
	else:
		log_error("%s plugin not initialized" % PLUGIN_SINGLETON_NAME)


func _on_review_info_generated() -> void:
	emit_signal(SIGNAL_NAME_REVIEW_INFO_GENERATED)


func _on_review_info_generation_failed() -> void:
	emit_signal(SIGNAL_NAME_REVIEW_INFO_GENERATION_FAILED)


func _on_review_flow_launched() -> void:
	emit_signal(SIGNAL_NAME_REVIEW_FLOW_LAUNCHED)


func _on_review_flow_launch_failed() -> void:
	emit_signal(SIGNAL_NAME_REVIEW_FLOW_LAUNCH_FAILED)


func _on_app_review_url_ready(a_url: String) -> void:
	emit_signal(SIGNAL_NAME_APP_REVIEW_URL_READY, a_url)


func _on_get_app_review_url_failed() -> void:
	emit_signal(SIGNAL_NAME_GET_APP_REVIEW_URL_FAILED)


static func log_error(a_description: String) -> void:
	push_error(a_description)


static func log_info(a_description: String) -> void:
	print_rich("[color=cyan]INFO: %s[/color]" % a_description)
