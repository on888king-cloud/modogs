Optional animated player model
==============================

Put one skinned character model containing both Idle and Run animations here:

    assets/Models/player.glb

The game uses jMonkeyEngine's glTF loader and AnimComposer. At startup it
prints available animation clip names to the console and looks for names
containing "idle" (or "stand"/"breathing") and "run" (or "walk"). Idle plays
when the character is still; Run/Running/Walk plays while moving. If no model
is present, the game builds an articulated low-poly character and animates its
limbs procedurally in Java (idle breathing, running gait, and attack swing).

Mixamo models and animation workflow
------------------------------------

jMonkeyEngine does not load Mixamo FBX files directly in this project. Convert
the character and animations to glTF/GLB in Blender:

1. Download the character and Idle/Run animations from Mixamo using the same
   skeleton. Import the FBX files into Blender.
2. Keep the character's armature and make sure the Idle and Run actions are
   assigned to that same armature. Name the actions "Idle" and "Running".
   If the imported files created extra armatures, transfer their actions to
   the character armature first; otherwise those clips will not animate the
   player mesh.
3. In Blender's glTF 2.0 exporter, include the mesh, skin/armature, and
   animations. Export as glTF Binary (.glb) to the path above. Ensure both
   animation actions are included by the exporter.
4. Run the game and check the startup console for "Character animation
   clips". If the clip names differ, rename the actions in Blender to Idle
   and Running, export again, and replace player.glb.

Controls: WASD / arrow keys to move; Shift to sprint. The character switches
between Idle and Run automatically from movement input.

Combat collision prototype
--------------------------

The current 3D prototype uses lightweight game-side colliders rather than a
rigid-body physics engine: the player is treated as an upright capsule, trees
and rocks block movement, and monsters have an AABB target collider. Space
creates a forward-facing weapon hitbox; monster health is reduced only when
that hitbox intersects the target collider. A monster that reaches the
player deals contact damage. This keeps the prototype dependency-free beyond
jMonkeyEngine and is suitable for simple melee combat, not physical simulation.

HUD and audio
-------------

The HUD shows the player's HP bar, score, defeated-monster count, and current
monster health. Each defeated monster awards 100 points. GameAudio.java
generates a looping background melody and short swing, hit, healing, and
reward sounds as PCM samples using Java Sound; no audio files or extra
dependencies are needed. If the system has no audio mixer, the game displays
an audio initialization warning in the HUD. Close the game window to stop and
release the audio clips.

Game flow
---------

The game opens at a mouse-clickable main menu. Choose "Start Adventure" to
enter play mode. Defeating enemies awards points but does not end the game;
when the player loses all HP, they respawn at the blue faction base after five
seconds. The result screen offers restart and return-to-menu buttons, and
restarting resets player stats and creates a new set of map events.

The gameplay map covers a 1000x1000 world area with an edge-based safe base for
each faction. Its square minimap shows the three factions, high-ground fire
positions, forward bases, the large control-area boundary, the moving hot zone,
and exploration events.

3D squad
--------

The playable 3D scene spawns 100 bots total (34 red, 33 green, and 33 blue) with
six jobs: Warriors specialize in durable melee and knockback; Mages deal
area damage; Archers attack at long range and keep their distance; Engineers
defend the hot zone; Logistics heal nearby allies and shorten nearby respawn
timers; Rogues move quickly and deal bonus damage to isolated targets; Priests
cast visible healing spells and floating HP numbers. Job markers are shown
above each bot.

Mages create a pulsing flame-storm area marker when their area attack lands.
Rogues can blink behind a target or away from danger when critically wounded;
their short shadow trail also draws enemy bots away as a decoy. Engineers
travel to the moving hot zone edge and spend 3-5 seconds stationary building
in sequence: a blocking wall, an automatically firing archer tower, and an
area-damage turret. Construction progress appears in the HUD; structures are
also marked on the minimap. Walls stop movement and projectiles.

All factions fight nearby enemies and will target the player when the player
approaches the hot zone. Attacks can knock back and briefly stun the player
or bots. Trees are decorative and do not block the player or bots, preventing
trunks and canopies from trapping the squad. Rocks remain solid; bots can
detour or make a short emergency reposition if stuck, and player movement
slides along obstacles. Dead bots respawn after five seconds. The
minimap, score HUD, and
combat log show the three factions and hill-control match; each 30-second
control check awards a point to the faction with the most weighted votes.
The first faction to reach 100 points wins.

Each faction has a marked safe spawn base near a different edge of the map.
Warriors and Rogues push toward the moving hot zone. Archers, Mages, and
Priests use marked high ground to establish ranged firing positions.
Engineers and Logistics units escort the forward-base builder to a strategic
position between their home base and the control area. Once a faction's
Engineer or Logistics unit reaches the site, it takes 12 seconds to complete
the FOB. Afterward, that faction's bots respawn there after two seconds rather
than at the rear base after five seconds. Enemy bots cannot enter another
faction's safe base or damage its defenders while they remain inside. The
player respawns after five seconds with three seconds of additional protection.
Archer attacks now create visible, moving arrows that must reach their target
before dealing damage instead of applying damage instantly.

Each faction has up to three Logistics drivers, each with a faction-colored
horse-drawn transport wagon traveling at 1.5 times normal walking speed. Every
wagon has a five-unit capacity including its driver. Nearby allied bots walk
to the pickup point and board; the wagon announces departure and arrival in
the combat log, drives to the moving hot-zone edge, then unloads its riders to
fight. It returns to its faction base and repeats the shuttle cycle. The
minimap shows wagon positions and their current routes.
Each driver AI independently chooses its next route among the hot zone, its
faction's FOB, and a drop-off point outside the nearest enemy safe base. The
decision weighs nearby friendly/enemy strength, FOB status, and the current
routes of the faction's other wagons so the convoy can split its deployments.
The chosen destination and each wagon's route are shown in the HUD/minimap.

The independent 30-unit, three-faction terminal simulation remains available
with `mvn -Pwar-simulation compile exec:java`.
The king-of-the-hill terminal simulation maintains 30 faction slots with
five-second respawns, a moving hot zone, a 50x50 map, and 30-second weighted
score checks. Run it with `mvn -Pking-of-the-hill compile exec:java`;
`--fast --quiet` skips real-time delays and map rendering for quick simulation
tests. The live match runs until a faction reaches 100 points; append
`--max-seconds=300` after `-Dexec.args=` to stop a test run after a chosen
number of simulated seconds.

Run the game from the project root:

    mvn compile exec:java

The original Swing RPG remains available from Main.java.
