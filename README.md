# Smeargle's Mystery Pixel Art

Cobblemon event specification for **Fabric 1.21.1** and **Cobblemon 1.7.0** using **MiniMessage** for all player-facing text.

## 1. Event Overview

Smeargle's Mystery Pixel Art is a competitive guessing event where an AI-controlled Smeargle progressively builds a pixel-art Pokémon from a predefined schematic.

Players watch the image slowly appear and try to identify the Pokémon as quickly as possible. Correct guesses award points each round, with earlier correct answers worth more.

### Core gameplay loop

**Admins open a registration window → players join → Smeargle starts building → players submit `/guess <pokemon>`**

## 2. Platform Target

- **Minecraft:** Fabric 1.21.1
- **Cobblemon:** 1.7.0
- **Text formatting:** MiniMessage

## 3. Round Structure

Each round uses one predefined Pokémon pixel-art schematic.

1. An admin opens session registration.
2. Players register with `/smearglesjoin` during a 10-minute signup window.
3. If players joined, the first round starts automatically when signup expires (or earlier with admin force-start).
4. The event selects a hidden target Pokémon tied to the schematic.
5. Smeargle places blocks gradually rather than revealing the image instantly.
6. Players attempt to identify the Pokémon while the build is still incomplete.
7. Players submit guesses with `/guess <pokemon>`.
8. Correct guesses award points (first correct = 10, then decreasing to a minimum of 1).
9. If nobody guesses correctly before the build finishes, the round ends with the answer revealed.
10. After the configured number of rounds, player totals are compared and the top scorer(s) win.

## 4. Gameplay Requirements

### 4.1 Smeargle builder behaviour

- Smeargle should feel like the active artist for the event.
- The build must be revealed progressively, one step at a time, from the selected schematic.
- The reveal pace should be slow enough for deduction and fast enough to keep rounds engaging.
- Only blocks defined by the schematic should be placed during a round.

### 4.2 Guessing rules

- Guesses are submitted with `/guess <pokemon>`.
- Guess matching should accept the intended Pokémon name in a player-friendly way.
- Correct guesses score points by order (10 down to 1 minimum).
- Incorrect guesses must not end the round.
- Only one Pokémon answer is valid for each round.

### 4.3 Win condition

- The final winner is decided after the configured round count is completed.
- Players with the highest total score at the end are declared the winner(s).

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
- awarding ordered points for correct guesses,
- determining end-of-session winner(s) from cumulative score,
- announcing the result with MiniMessage text.

## 8. Current Implementation

This repository now contains an initial server-side Fabric sidemod implementation that targets Cobblemon 1.7.0 on Minecraft 1.21.1.

### Admin commands

- `/smearglespixelart start random [rounds]` starts a game session using random templates (default: 1 round).
- `/smearglespixelart start template <template> [rounds]` starts a game session with the named template first, then uses random templates for remaining rounds (default: 1 round).
- `/smearglespixelart force-start` closes an active registration window early and starts immediately when players are registered.
- `/smearglespixelart reload` reloads the Smeargle configuration file.
- `/smearglespixelart record <template> <from> <to> <pokemon>` records a 2D sprite selection from the world into a reusable template JSON file.
- `/smearglespixelart list` shows available built-in templates.
- `/smearglespixelart status` shows current round progress.
- `/smearglespixelart stop` stops the active round.

### Player command

- `/guess <pokemon>` submits a guess for the active round.
- `/smearglesjoin` registers the player during an active 10-minute session signup window.

### Round hints

- Each round starts with a name-length hint.
- Additional MiniMessage hints are broadcast as Smeargle reveals more of the sprite.
- Later hints reveal the first letter and then a simple name pattern.
- Painting and cleanup now use the placed blocks' own sounds, while round starts, correct guesses, and anger spikes play extra audio cues.
- As the painting gets closer to completion, Smeargle's anger rises in quarter-step stages.
- At each anger spike he now builds back down first if he is elevated, lingers much longer between phases, walks forward toward the audience while facing them, looks back at the art, steps back toward the canvas, then surges forward before resuming work.
- Higher frustration stages lengthen that animation and add extra jumps plus angry particles.

### Round cleanup

- After a round ends, Smeargle waits 5 seconds before clearing the sprite.
- Cleanup then happens on the configured canvas at roughly 5x the normal painting pace.
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
- Smeargle now paints from the front of the canvas, repositions before climbing temporary scaffolding upward one step at a time for taller sections, climbs back down when needed, and removes that support without dropping items.
- Smeargle's native mob AI is disabled during rounds so he only moves when the event logic tells him to.

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
