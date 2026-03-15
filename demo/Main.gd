#
# © 2024-present https://github.com/cengiz-pz
#

extends Node

@onready var inapp_review: InappReview = $InappReview as InappReview
@onready var _label: RichTextLabel = $CanvasLayer/CenterContainer/VBoxContainer/RichTextLabel as RichTextLabel
@onready var _android_texture_rect := %AndroidTextureRect as TextureRect
@onready var _ios_texture_rect := %iOSTextureRect as TextureRect

var _active_texture_rect: TextureRect


func _ready() -> void:
	if OS.has_feature("ios"):
		_android_texture_rect.hide()
		_active_texture_rect = _ios_texture_rect
	else:
		_ios_texture_rect.hide()
		_active_texture_rect = _android_texture_rect


func _on_launch_review_button_pressed() -> void:
	inapp_review.generate_review_info()


func _on_get_review_url_button_pressed() -> void:
	inapp_review.get_app_review_url()


func _on_inapp_review_review_info_generated() -> void:
	_print_to_screen("In-app review info generation success!")
	_print_to_screen("Launching review flow...")
	inapp_review.launch_review_flow()


func _on_inapp_review_review_info_generation_failed() -> void:
	_print_to_screen("In-app review info generation failed!", true)


func _on_inapp_review_review_flow_launched() -> void:
	_print_to_screen("In-app review flow launched!")


func _on_inapp_review_review_flow_launch_failed() -> void:
	_print_to_screen("In-app review flow launch failed!", true)


func _on_inapp_review_app_review_url_ready(a_url: String) -> void:
	_print_to_screen("App review URL: %s" % a_url)


func _on_inapp_review_get_app_review_url_failed() -> void:
	_print_to_screen("Failed to get app review URL", true)


func _print_to_screen(a_message: String, a_is_error: bool = false) -> void:
	_label.add_text("%s\n\n" % a_message)
	if a_is_error:
		printerr(a_message)
	else:
		print(a_message)
