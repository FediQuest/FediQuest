extends Node
class_name GeoUtils

## FediQuest GPS/Geolocation Utilities for Godot
## Handles GPS coordinate conversion and distance calculations
## Matches functionality from web/js/spawn-loader.js

signal location_updated(location: Dictionary)
signal location_failed(error: String)

const EARTH_RADIUS_METERS := 6371000.0

var current_location: Dictionary = {}
var geolocator: Geolocation  # Godot 4.x geolocation node (if available)

func _ready() -> void:
	# Try to initialize geolocation if available
	if ClassDB.class_exists("Geolocation"):
		geolocator = Geolocation.new()
		add_child(geolocator)
		# Note: Actual geolocation implementation depends on platform

## Request GPS location (platform-dependent)
func request_location() -> void:
	# Platform-specific implementations would go here
	# For now, this is a placeholder showing the interface
	
	# On Android, you would use a plugin or Godot's experimental geolocation
	# On desktop, GPS is typically not available
	
	push_warning("GeoUtils.request_location(): Platform geolocation not implemented")
	print("[GeoUtils] Using mock location for development")
	
	# Mock location for testing (NYC coordinates)
	current_location = {
		"latitude": 40.7128,
		"longitude": -74.0060,
		"accuracy": 10.0,
		"timestamp": Time.get_unix_time_from_system()
	}
	location_updated.emit(current_location)

## Start continuous location tracking
func start_tracking(update_interval_ms: int = 5000) -> void:
	# Placeholder for continuous tracking
	# Would use platform-specific location services
	pass

## Stop location tracking
func stop_tracking() -> void:
	pass

## Calculate distance between two GPS coordinates using Haversine formula
func calculate_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
	var phi1 := deg_to_rad(lat1)
	var phi2 := deg_to_rad(lat2)
	var delta_phi := deg_to_rad(lat2 - lat1)
	var delta_lambda := deg_to_rad(lon2 - lon1)
	
	var a := sin(delta_phi / 2.0) * sin(delta_phi / 2.0) + \
			 cos(phi1) * cos(phi2) * \
			 sin(delta_lambda / 2.0) * sin(delta_lambda / 2.0)
	
	var c := 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
	
	return EARTH_RADIUS_METERS * c

## Convert degrees to radians
func deg_to_rad(degrees: float) -> float:
	return degrees * (PI / 180.0)

## Convert GPS coordinates to local scene position
## This is a simplified conversion - real implementation needs proper UTM projection
func gps_to_local(latitude: float, longitude: float, reference_lat: float = 0.0, reference_lon: float = 0.0) -> Vector3:
	# Use reference point as origin (0, 0, 0)
	if reference_lat == 0.0 and reference_lon == 0.0:
		# Default to first spawn location as reference
		reference_lat = 40.7128
		reference_lon = -74.0060
	
	# Calculate offset in meters from reference point
	var x_offset := calculate_distance(reference_lat, reference_lon, reference_lat, longitude)
	if longitude < reference_lon:
		x_offset = -x_offset
	
	var z_offset := calculate_distance(reference_lat, reference_lon, latitude, reference_lon)
	if latitude < reference_lat:
		z_offset = -z_offset
	
	return Vector3(x_offset, 0.0, z_offset)

## Get bearing between two points (in degrees)
func get_bearing(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
	var phi1 := deg_to_rad(lat1)
	var phi2 := deg_to_rad(lat2)
	var delta_lambda := deg_to_rad(lon2 - lon1)
	
	var y := sin(delta_lambda) * cos(phi2)
	var x := cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(delta_lambda)
	
	var bearing := atan2(y, x)
	bearing = rad_to_deg(bearing)
	
	# Normalize to 0-360
	return fmod((bearing + 360.0), 360.0)

## Interpolate between two GPS points
func interpolate_gps(lat1: float, lon1: float, lat2: float, lon2: float, t: float) -> Dictionary:
	# Simple linear interpolation (not great-circle, but sufficient for short distances)
	var lat := lerp(lat1, lat2, clamp(t, 0.0, 1.0))
	var lon := lerp(lon1, lon2, clamp(t, 0.0, 1.0))
	
	return {
		"latitude": lat,
		"longitude": lon
	}

## Check if a point is within radius of another point
func is_within_radius(lat1: float, lon1: float, lat2: float, lon2: float, radius_meters: float) -> bool:
	return calculate_distance(lat1, lon1, lat2, lon2) <= radius_meters

## Format GPS coordinates for display
func format_coordinates(latitude: float, longitude: float, precision: int = 6) -> String:
	var lat_dir := "N" if latitude >= 0 else "S"
	var lon_dir := "E" if longitude >= 0 else "W"
	
	return "%.*f°%s %.*f°%s" % [
		precision, abs(latitude), lat_dir,
		precision, abs(longitude), lon_dir
	]

## Parse GPS coordinates from string
func parse_coordinates(coord_string: String) -> Dictionary:
	# Expected format: "40.7128,-74.0060" or "40.7128 N, 74.0060 W"
	var parts := coord_string.split(",")
	if parts.size() < 2:
		return {"error": "Invalid coordinate format"}
	
	var lat := float(parts[0].strip_edges())
	var lon := float(parts[1].strip_edges())
	
	return {
		"latitude": lat,
		"longitude": lon
	}

## Get current cached location
func get_current_location() -> Dictionary:
	return current_location.duplicate()

## Set location manually (for testing/debugging)
func set_mock_location(latitude: float, longitude: float, accuracy: float = 10.0) -> void:
	current_location = {
		"latitude": latitude,
		"longitude": longitude,
		"accuracy": accuracy,
		"timestamp": Time.get_unix_time_from_system(),
		"is_mock": true
	}
	location_updated.emit(current_location)
