#!/usr/bin/env python3
"""
FediQuest Overpass API Quest Generator

Generates quests from OpenStreetMap data using the Overpass API.
Supports multiple quest types based on OSM tags.

Usage:
    python overpass_generator.py --lat 40.7128 --lon -74.0060 --radius 1000
"""

import argparse
import json
import requests
from typing import List, Dict, Any
from datetime import datetime


class OverpassQuestGenerator:
    """Generate FediQuest quests from OpenStreetMap data."""
    
    OVERPASS_URL = "https://overpass-api.de/api/interpreter"
    
    # Quest type mappings from OSM tags
    QUEST_TEMPLATES = {
        'recycling': {
            'tags': ['amenity=recycling', 'amenity=waste_basket'],
            'type': 'RECYCLING',
            'xp': 75,
            'title_template': 'Recycling Station at {name}',
            'description_template': 'Sort recyclables at {name}. Proper recycling reduces landfill waste.'
        },
        'park': {
            'tags': ['leisure=park', 'landuse=grass', 'landuse=meadow'],
            'type': 'CLEANUP',
            'xp': 50,
            'title_template': 'Park Cleanup at {name}',
            'description_template': 'Help keep {name} clean for everyone to enjoy!'
        },
        'water': {
            'tags': ['waterway=river', 'waterway=lake', 'amenity=drinking_water'],
            'type': 'WATER',
            'xp': 90,
            'title_template': 'Water Conservation at {name}',
            'description_template': 'Check water quality and conservation at {name}.'
        },
        'forest': {
            'tags': ['landuse=forest', 'natural=wood', 'natural=tree_row'],
            'type': 'PLANTING',
            'xp': 100,
            'title_template': 'Tree Planting Zone near {name}',
            'description_template': 'Plant trees in this area to support local ecosystem.'
        },
        'garden': {
            'tags': ['leisure=garden', 'landuse=allotments'],
            'type': 'WILDFLOWER',
            'xp': 120,
            'title_template': 'Wildflower Garden at {name}',
            'description_template': 'Plant native wildflowers at {name} to support pollinators!'
        }
    }
    
    def __init__(self, lat: float, lon: float, radius: int = 1000):
        self.lat = lat
        self.lon = lon
        self.radius = radius
        
    def build_overpass_query(self) -> str:
        """Build Overpass QL query for all quest types."""
        tag_conditions = []
        
        for category, template in self.QUEST_TEMPLATES.items():
            for tag in template['tags']:
                key, value = tag.split('=')
                tag_conditions.append(f'node["{key}"="{value}"](around:{self.radius},{self.lat},{self.lon});')
                tag_conditions.append(f'way["{key}"="{value}"](around:{self.radius},{self.lat},{self.lon});')
        
        query = f"""
        [out:json][timeout:25];
        (
            {' '.join(tag_conditions)}
        );
        out center;
        """
        return query
    
    def fetch_osm_data(self) -> List[Dict[str, Any]]:
        """Fetch data from Overpass API."""
        query = self.build_overpass_query()
        
        try:
            response = requests.post(
                self.OVERPASS_URL,
                data={'data': query},
                headers={'User-Agent': 'FediQuest/1.0'}
            )
            response.raise_for_status()
            data = response.json()
            return data.get('elements', [])
        except requests.RequestException as e:
            print(f"Error fetching OSM data: {e}")
            return []
    
    def generate_quests(self) -> List[Dict[str, Any]]:
        """Generate quests from OSM elements."""
        osm_elements = self.fetch_osm_data()
        quests = []
        seen_ids = set()
        
        for element in osm_elements:
            # Get coordinates
            if element['type'] == 'node':
                lat = element['lat']
                lon = element['lon']
            elif element['type'] == 'way' and 'center' in element:
                lat = element['center']['lat']
                lon = element['center']['lon']
            else:
                continue
            
            # Determine quest type from tags
            tags = element.get('tags', {})
            quest_info = self._match_quest_type(tags)
            
            if quest_info and element['id'] not in seen_ids:
                seen_ids.add(element['id'])
                
                name = tags.get('name', f"Location {element['id']}")
                category, template = quest_info
                
                quest = {
                    'id': f"osm_{element['id']}_{datetime.now().timestamp()}",
                    'latitude': lat,
                    'longitude': lon,
                    'modelUrl': f"models/{category.lower()}.glb",
                    'etag': f'"v1-{element["id"]}-{int(datetime.now().timestamp())}"',
                    'metadata': {
                        'name': template['title_template'].format(name=name),
                        'description': template['description_template'].format(name=name),
                        'type': template['type'],
                        'xpReward': template['xp'],
                        'companionHint': self._get_companion_hint(template['type']),
                        'environmentalImpact': self._get_impact_description(template['type']),
                        'osmId': element['id'],
                        'osmTags': tags
                    }
                }
                quests.append(quest)
        
        return quests
    
    def _match_quest_type(self, tags: Dict[str, str]):
        """Match OSM tags to quest type."""
        for category, template in self.QUEST_TEMPLATES.items():
            for tag in template['tags']:
                key, value = tag.split('=')
                if tags.get(key) == value:
                    return category, template
        return None
    
    def _get_companion_hint(self, quest_type: str) -> str:
        """Get companion hint based on quest type."""
        hints = {
            'PLANTING': '🐝 Busy Bee loves helping plant trees!',
            'RECYCLING': '🦊 Forest Fox appreciates your recycling efforts!',
            'CLEANUP': '🐢 Sea Turtle thanks you for cleaning up!',
            'WILDFLOWER': '🐝 Busy Bee is excited about wildflowers!',
            'WATER': '🐢 Sea Turtle guides you to protect waterways!',
            'WILDLIFE': '🐦 Song Bird wants to help you build habitats!'
        }
        return hints.get(quest_type, 'A companion is watching your progress!')
    
    def _get_impact_description(self, quest_type: str) -> str:
        """Get environmental impact description."""
        impacts = {
            'PLANTING': '1 tree planted = ~48 lbs CO2 absorbed per year',
            'RECYCLING': '1 recycling trip = ~5 lbs waste diverted from landfills',
            'CLEANUP': '1 cleanup event = ~10 lbs trash removed',
            'WILDFLOWER': '100 wildflowers = support for 1000s of bees and butterflies',
            'WATER': '1 water action = ~100 liters water saved/protected',
            'WILDLIFE': '1 birdhouse = home for 1 bird family per season'
        }
        return impacts.get(quest_type, 'Your actions make a positive difference!')
    
    def save_to_json(self, quests: List[Dict], output_file: str):
        """Save generated quests to JSON file."""
        output = {
            'spawns': quests,
            'lastUpdated': datetime.utcnow().isoformat() + 'Z',
            'version': '1.0.0',
            'generatedBy': 'OverpassQuestGenerator',
            'location': {
                'latitude': self.lat,
                'longitude': self.lon,
                'radiusMeters': self.radius
            },
            'gameInfo': {
                'description': 'FediQuest encourages people to go outside, help each other, and do something good for the environment.',
                'rewards': 'Earn XP, unlock avatar skins, discover companions, and track your environmental impact!',
                'fediverseIntegration': 'Share your achievements to any ActivityPub-compatible instance'
            }
        }
        
        with open(output_file, 'w') as f:
            json.dump(output, f, indent=2)
        
        print(f"Generated {len(quests)} quests → {output_file}")


def main():
    parser = argparse.ArgumentParser(description='Generate FediQuest quests from OpenStreetMap')
    parser.add_argument('--lat', type=float, required=True, help='Latitude center point')
    parser.add_argument('--lon', type=float, required=True, help='Longitude center point')
    parser.add_argument('--radius', type=int, default=1000, help='Search radius in meters')
    parser.add_argument('--output', type=str, default='generated_quests.json', help='Output JSON file')
    
    args = parser.parse_args()
    
    generator = OverpassQuestGenerator(args.lat, args.lon, args.radius)
    quests = generator.generate_quests()
    generator.save_to_json(quests, args.output)
    
    print(f"\nQuest Generation Summary:")
    print(f"  Location: {args.lat}, {args.lon}")
    print(f"  Radius: {args.radius}m")
    print(f"  Quests Generated: {len(quests)}")
    print(f"  Output File: {args.output}")


if __name__ == '__main__':
    main()
