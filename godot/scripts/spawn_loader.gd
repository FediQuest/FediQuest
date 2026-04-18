extends Node
class_name SpawnLoader

## FediQuest Spawn Loader for Godot
## Fetches spawn data from server.json and places 3D models at GPS coordinates
## This is a GDScript implementation matching the web demo functionality

signal spawns_loaded(spawns: Array)
signal spawns_failed(error: String)
signal cache_cleared()

const CACHE_FILE := "user://spawn_cache.json"
const ETAG_FILE := "user://spawn_etag.txt"

var spawns: Array = []
var http_request: HTTPRequest
var cached_etag: String = ""

func _ready() -> void:
	http_request = HTTPRequest.new()
	add_child(http_request)
	http_request.request_completed.connect(_on_request_completed)
	
	# Load cached ETag if exists
	_load_cached_etag()

## Fetch spawn data from server with ETag caching
func fetch_spawns(server_url: String = "res://../server/server.json") -> Error:
	var headers := ["Accept: application/json"]
	
	if not cached_etag.is_empty():
		headers.append("If-None-Match: " + cached_etag)
	
	var error := http_request.request(server_url, headers, HTTPClient.METHOD_GET)
	if error != OK:
		push_error("Failed to request spawns: " + str(error))
		return error
	
	return OK

func _on_request_completed(result: int, response_code: int, headers: PackedStringArray, body: PackedByteArray) -> void:
	# Handle 304 Not Modified
	if response_code == 304:
		print("[SpawnLoader] Data not modified, using cached version")
		_load_cached_spawns()
		cache_cleared.emit()
		return
	
	# Handle successful response
	if response_code == 200:
		var json := JSON.new()
		var parse_error := json.parse(body.get_string_from_utf8())
		
		if parse_error != OK:
			push_error("Failed to parse spawn JSON: " + str(parse_error))
			spawns_failed.emit("JSON parse error")
			return
		
		var data := json.get_data()
		spawns = data.get("spawns", [])
		
		# Store ETag for future requests
		for header in headers:
			if header.begins_with("ETag:"):
				cached_etag = header.substr(6).strip_edges()
				_save_etag()
				break
		
		# Cache the data
		_cache_spawns(data)
		
		print("[SpawnLoader] Loaded %d spawns" % spawns.size())
		spawns_loaded.emit(spawns)
		return
	
	# Handle errors
	push_error("Spawn request failed: %d" % response_code)
	spawns_failed.emit("HTTP %d" % response_code)

## Place spawns in the 3D scene
func place_spawns_in_scene(parent: Node, model_base_path: String = "res://assets/models/") -> void:
	# Clear existing spawns
	for child in parent.get_children():
		if child.has_meta("_spawn_entity"):
			child.queue_free()
	
	# Create new spawn instances
	for spawn_data in spawns:
		var spawn_node := _create_spawn_node(spawn_data, model_base_path)
		if spawn_node:
			parent.add_child(spawn_node)

func _create_spawn_node(spawn_data: Dictionary, model_base_path: String) -> Node3D:
	var spawn_id := spawn_data.get("id", "unknown")
	var latitude := float(spawn_data.get("latitude", 0))
	var longitude := float(spawn_data.get("longitude", 0))
	var model_url := spawn_data.get("modelUrl", "")
	var metadata := spawn_data.get("metadata", {})
	
	# Convert GPS to local coordinates (simplified - use GeoUtils for real implementation)
	var local_pos := GeoUtils.gps_to_local(latitude, longitude)
	
	# Create node container
	var spawn_node := Node3D.new()
	spawn_node.set_meta("_spawn_entity", true)
	spawn_node.set_meta("_spawn_id", spawn_id)
	spawn_node.set_meta("_spawn_data", spawn_data)
	spawn_node.position = local_pos
	
	# Load and attach 3D model
	var model_path := model_base_path + model_url.get_file()
	if ResourceLoader.exists(model_path):
		var model := load(model_path) as PackedScene
		if model:
			var instance := model.instantiate()
			spawn_node.add_child(instance)
	else:
		# Use placeholder if model not found
		var placeholder := MeshInstance3D.new()
		placeholder.mesh = BoxMesh.new()
		placeholder.scale = Vector3(0.5, 0.5, 0.5)
		spawn_node.add_child(placeholder)
		
		push_warning("Model not found: " + model_path + " (using placeholder)")
	
	return spawn_node

## Cache management
func _cache_spawns(data: Dictionary) -> void:
	var file := FileAccess.open(CACHE_FILE, FileAccess.WRITE)
	if file:
		var json := JSON.stringify(data)
		file.store_string(json)
		file.close()

func _load_cached_spawns() -> void:
	if not FileAccess.file_exists(CACHE_FILE):
		return
	
	var file := FileAccess.open(CACHE_FILE, FileAccess.READ)
	if file:
		var json := JSON.new()
		var parse_error := json.parse(file.get_as_text())
		file.close()
		
		if parse_error == OK:
			var data := json.get_data()
			spawns = data.get("spawns", [])
			spawns_loaded.emit(spawns)

func _save_etag() -> void:
	var file := FileAccess.open(ETAG_FILE, FileAccess.WRITE)
	if file:
		file.store_string(cached_etag)
		file.close()

func _load_cached_etag() -> void:
	if FileAccess.file_exists(ETAG_FILE):
		var file := FileAccess.open(ETAG_FILE, FileAccess.READ)
		if file:
			cached_etag = file.get_as_text().strip_edges()
			file.close()

func clear_cache() -> void:
	if FileAccess.file_exists(CACHE_FILE):
		DirAccess.remove_absolute(CACHE_FILE)
	if FileAccess.file_exists(ETAG_FILE):
		DirAccess.remove_absolute(ETAG_FILE)
	
	spawns.clear()
	cached_etag = ""
	cache_cleared.emit()
	print("[SpawnLoader] Cache cleared")

func get_cache_info() -> Dictionary:
	return {
		"has_cache": FileAccess.file_exists(CACHE_FILE),
		"has_etag": not cached_etag.is_empty(),
		"etag": cached_etag,
		"spawn_count": spawns.size()
	}
