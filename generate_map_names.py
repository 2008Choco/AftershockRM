# Uses ballchasing.com's API to generate a key-value pair of Rocket League's internal map ids to their human-readable names

import requests

API_TOKEN = "OL7OnyGIg3SAtdeRtaUjK1qpPownKyunL8oinrFv"
USER_AGENT = "AftershockRM_MapNameGenerator/1.0 (+https://github.com/2008Choco/AftershockRM)"

ENDPOINT = "https://ballchasing.com/api/maps"
EXPORT_FILE_NAME = "map_names.properties"

def main():
    print("Fetching map names!")
    headers = { "Authorization": API_TOKEN, "User-Agent": USER_AGENT }
    map_names = requests.get(ENDPOINT, headers=headers).json()

    print(f'Found ({len(map_names)}) map names. Dumping to {EXPORT_FILE_NAME}!')
    with open(EXPORT_FILE_NAME, "w") as file:
        for key, value in map_names.items():
            file.write(f'map.name.{key}: {value}\n')
    print("Done! Thank you, Ballchasing!")

if __name__ == "__main__":
    main()
