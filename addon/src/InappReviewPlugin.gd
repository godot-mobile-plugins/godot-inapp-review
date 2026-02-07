#
# © 2024-present https://github.com/cengiz-pz
#

@tool
extends EditorPlugin

const PLUGIN_NAME: String = "@pluginName@"
const PLUGIN_PACKAGE: String = "@pluginPackage@"
const ANDROID_DEPENDENCIES: Array = [ @androidDependencies@ ]
const IOS_FRAMEWORKS: Array = [ @iosFrameworks@ ]
const IOS_EMBEDDED_FRAMEWORKS: Array = [ @iosEmbeddedFrameworks@ ]
const IOS_LINKER_FLAGS: Array = [ @iosLinkerFlags@ ]

var android_export_plugin: AndroidExportPlugin
var ios_export_plugin: IosExportPlugin


func _enter_tree() -> void:
	android_export_plugin = AndroidExportPlugin.new()
	add_export_plugin(android_export_plugin)
	ios_export_plugin = IosExportPlugin.new()
	add_export_plugin(ios_export_plugin)


func _exit_tree() -> void:
	remove_export_plugin(android_export_plugin)
	android_export_plugin = null
	remove_export_plugin(ios_export_plugin)
	ios_export_plugin = null


class AndroidExportPlugin extends EditorExportPlugin:
	var _plugin_name = PLUGIN_NAME


	func _supports_platform(platform: EditorExportPlatform) -> bool:
		if platform is EditorExportPlatformAndroid:
			return true
		return false


	func _get_android_libraries(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		if debug:
			return PackedStringArray(["%s/bin/debug/%s-debug.aar" % [_plugin_name, _plugin_name]])
		else:
			return PackedStringArray(["%s/bin/release/%s-release.aar" % [_plugin_name, _plugin_name]])


	func _get_name() -> String:
		return _plugin_name


	func _get_android_dependencies(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		return PackedStringArray(ANDROID_DEPENDENCIES)


class IosExportPlugin extends EditorExportPlugin:
	var _plugin_name = PLUGIN_NAME


	func _supports_platform(platform: EditorExportPlatform) -> bool:
		if platform is EditorExportPlatformIOS:
			return true
		return false


	func _get_name() -> String:
		return _plugin_name


	func _export_begin(features: PackedStringArray, is_debug: bool, path: String, flags: int) -> void:
		for __framework in IOS_FRAMEWORKS:
			add_apple_embedded_platform_framework(__framework)

		for __framework in IOS_EMBEDDED_FRAMEWORKS:
			add_apple_embedded_platform_embedded_framework(__framework)

		for __flag in IOS_LINKER_FLAGS:
			add_apple_embedded_platform_linker_flags(__flag)
