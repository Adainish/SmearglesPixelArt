# Smeargle's Mystery Pixel Art

Cobblemon event specification for **Fabric 1.21.1** and **Cobblemon 1.7.0** using **MiniMessage** for all player-facing text.

## 1. Event Overview

Smeargle's Mystery Pixel Art is a competitive guessing event where an AI-controlled Smeargle progressively builds a pixel-art Pokémon from a predefined schematic.

Players watch the image slowly appear and try to identify the Pokémon as quickly as possible. The first player to correctly guess the Pokémon wins the round.

### Core gameplay loop

**Smeargle starts building → blocks are revealed progressively → players recognise the Pokémon → players submit `/guess <pokemon>`**

## 2. Platform Target

- **Minecraft:** Fabric 1.21.1
- **Cobblemon:** 1.7.0
- **Text formatting:** MiniMessage

## 3. Round Structure

Each round uses one predefined Pokémon pixel-art schematic.

1. A round starts and announces that Smeargle is beginning a new drawing.
2. The event selects a hidden target Pokémon tied to the schematic.
3. Smeargle places blocks gradually rather than revealing the image instantly.
4. Players attempt to identify the Pokémon while the build is still incomplete.
5. Players submit guesses with `/guess <pokemon>`.
6. The first correct guess immediately ends the round and declares the winner.
7. If nobody guesses correctly before the build finishes, the round ends with the answer revealed.

## 4. Gameplay Requirements

### 4.1 Smeargle builder behaviour

- Smeargle should feel like the active artist for the event.
- The build must be revealed progressively, one step at a time, from the selected schematic.
- The reveal pace should be slow enough for deduction and fast enough to keep rounds engaging.
- Only blocks defined by the schematic should be placed during a round.

### 4.2 Guessing rules

- Guesses are submitted with `/guess <pokemon>`.
- Guess matching should accept the intended Pokémon name in a player-friendly way.
- The first correct guess wins the round.
- Incorrect guesses must not end the round.
- Only one Pokémon answer is valid for each round.

### 4.3 Win condition

- The winner is the first player whose guess matches the hidden Pokémon for the active round.
- When a player wins:
  - the round stops immediately,
  - the correct Pokémon is revealed,
  - the winner is broadcast to all players.

## 5. Messaging

All player-facing messages should use **MiniMessage** formatting.

Example message intents:

- Round start announcement
- Hints or progress updates
- Incorrect guess feedback
- Winner announcement
- Round end / reveal message

## 6. Content Requirements

- Pixel art must come from predefined schematics.
- Each schematic must map cleanly to a single Pokémon answer.
- The event should be able to run multiple rounds by selecting different valid schematics.

## 7. Functional Summary

The finished project should support:

- running a Smeargle-driven pixel-art guessing round,
- progressively placing schematic blocks,
- accepting player guesses through `/guess <pokemon>`,
- determining the first correct guess,
- announcing the result with MiniMessage text.

## 8. Current Implementation

This repository now contains an initial server-side Fabric sidemod implementation that targets Cobblemon 1.7.0 on Minecraft 1.21.1.

### Admin commands

- `/smearglespixelart start random` starts a round with a random built-in template on the configured canvas.
- `/smearglespixelart start template <template>` starts a round with a specific built-in template on the configured canvas.
- `/smearglespixelart reload` reloads the Smeargle configuration file.
- `/smearglespixelart record <template> <from> <to> <pokemon>` records a 2D sprite selection from the world into a reusable template JSON file.
- `/smearglespixelart list` shows available built-in templates.
- `/smearglespixelart status` shows current round progress.
- `/smearglespixelart stop` stops the active round.

### Player command

- `/guess <pokemon>` submits a guess for the active round.

### Round hints

- Each round starts with a name-length hint.
- Additional MiniMessage hints are broadcast as Smeargle reveals more of the sprite.
- Later hints reveal the first letter and then a simple name pattern.
- As the painting gets closer to completion, Smeargle's anger rises in quarter-step stages.
- At each anger spike he briefly turns toward nearby players before going back to work, and later stages add jumps plus angry particles.

### Round cleanup

- After a round ends, Smeargle waits 5 seconds before clearing the sprite.
- Cleanup then happens on the configured canvas using the same per-block pacing system.
- This delay leaves room for future end-of-round animations before the art is removed.

### Permissions

- Admin commands use the LuckPerms-compatible Fabric permissions API instead of operator levels.
- Grant access to the admin command set with:
  - `smearglespixelart.admin`
- Admin commands are denied by default unless a permissions provider such as LuckPerms grants that node.

### Configured canvas

- Rounds now paint on a configurable fixed canvas instead of taking a per-command build position.
- Edit `config/smearglespixelart/config.json` to choose:
  - the target dimension id,
  - the canvas origin,
  - the canvas direction,
  - the number of ticks Smeargle waits between block placements.
- Slower painting can be achieved by increasing `ticksPerPlacement`.
- Supported directions include cardinals and diagonals such as `north`, `east`, `southwest`, and `northwest`.
- Smeargle now builds temporary scaffolding up while painting taller sections and removes that support again as he comes back down.

### Built-in templates

- `pikachu`
- `voltorb`

The configured canvas origin is the bottom-left origin of the vertical pixel-art sprite.

### Recording new 2D sprite templates

- Recorded art must be a **single vertical 2D plane**:
  - either **X changes while Z stays constant**, or
  - **Z changes while X stays constant**.
- The recorder reads the selected rectangle from the world and writes a template file to:
  - `config/smearglespixelart/templates/<template>.json`
- Air is captured as transparent space in the sprite template.
- Saved templates are available for future rounds and follow the existing row/palette JSON format.
