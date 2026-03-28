# Playable Demo Instructions

This project includes a Swing-based text RPG launcher that exercises the core
Deliverable 2 use cases in one playable flow.

Run the demo (after building and copying dependencies):

```bash
./run.sh
```

Gameplay flow covered by the UI:
- Profile creation and loading.
- Hero creation and party management (up to 5 heroes).
- PvE campaign progression (`Start Campaign` + `Enter Next Room`) across room events.
- Battle outcomes with reward/penalty handling from campaign logic.
- Inn systems: party restore, item purchasing, recruit generation, recruit hiring.
- Party save slots for PvP use.
- PvP execution against a generated or existing opponent profile.
- League table display and Hall of Fame display.

Tips:
- Start in the `Profiles/Heroes` tab, then move to `Campaign/Inn`.
- Use `Show Status` often to inspect party stats and campaign progress.
- Save at least one party slot before running PvP.