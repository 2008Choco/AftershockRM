# TODO List
Included in this file are a list of features that I would like to introduce into Aftershock, or just things that I want to improve on. This file may be out of date over time as I implement changes from this list and I forget to update it, but this is a good reminder for what my plan is going forward.

## Features
- Add a file watcher to the live replay directory. Automatically import newly saved replays, automatically remove replays deleted from in-game, etc.
  - Replays added while Aftershock is open should be highlighted in the table!
- Add a way to allow users to restore recently deleted replays if they were deleted by accident.
- Automatically delete replays from the "recently deleted" folder after 30 days
- User-defined replay tags. Allow users to define a tag (name and colour) and assign them to replays for easy sorting and filtering
- Replay comments to define arbitrary text on a replay entry
- Add more menu bar and context menu buttons to perform common actions
- Goal timeline chart in the replay information tab (_maybe_)

## Rewrites
- When InputMappings are out of incubation (JavaFX), use these across the app.
  - This would hopefully allow for action definitions (i.e. "delete replay", "rename bin", etc.) that different inputs could map to
  - For example, define a "rename bin" action that can then be triggered with either the F2 key, a context menu button, or a menu bar button
- Improve the look-and-feel of the replay information tab. It's very barren, and has to be reconstructed for each replay. This sucks. Find a way to use property bindings on just one instance of a replay information tab
- Revisit ReplayBin. Improve the way it holds replays. Ideally it holds a weak reference to a replay by its ID, then resolves the replay from the global bin when necessary. See ReplayBinTypeAdapter for the source of this issue

## Bug Fixes
- Text in deletion confirmation dialogues are cut off, meaning the list of files or bins being deleted is not clear (_low priority_)
