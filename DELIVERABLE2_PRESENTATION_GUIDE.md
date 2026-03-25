# Deliverable 2 Presentation Guide

The grading rubric asks for two parts:

1. Demo of the application (max 5 minutes)
2. Architecture and design presentation (about 5 minutes)

You can satisfy this cleanly with:
- A console demo run from DemoMain
- A short slide deck showing architecture, design patterns, and diagrams

---

## Part A: 5-Minute Demo Script

### Demo setup

Run:

mvn clean test
mvn -q -DskipTests compile
java -cp target/classes app.DemoMain

### Demo flow (talk track)

1. Profile and hero creation
- Show creation of a profile and 3 heroes.
- Explain active party and save behavior.

2. Campaign progression (PvE)
- Show entering multiple rooms.
- Explain battle vs inn probability and rewards.

3. Inn behavior
- Show item purchase and recruitment attempt.
- Explain auto-revive and gold cost rules.

4. PvP battle
- Show second profile creation and saved party.
- Run PvP battle and explain no exp/gold reward in PvP.

5. League and Hall of Fame
- Show league table output.
- Show Hall of Fame ordering by high score.

### What to emphasize verbally
- "This demo covers UC1, UC6, UC7, and UC8 end-to-end."
- "Battle logic and abilities are reused across PvE and PvP through services."
- "All features shown are validated by automated tests."

---

## Part B: 5-Minute Design Presentation Script

Suggested slide structure:

1. Project scope and use cases
- List UC1 to UC8 briefly.

2. Architecture overview
- Controller layer
- Service layer
- Model layer
- Persistence layer

3. Major class interactions
- GameController as facade entrypoint
- BattleServiceImpl orchestration
- ProfileRepository for persistence abstraction

4. Six design patterns used
- Strategy: HeroClassStrategy and class strategies
- Template Method: Ability hierarchy
- Factory Method: Hero strategy creation
- Singleton: LeagueService
- Observer: HeroObserver notifications
- Repository/DAO: ProfileRepository and implementations

5. Verification and quality
- Current test pass count
- What key scenarios are covered
- Mention clean naming and comments updates

---

## 10 Manual Test Cases (TA-Friendly)

These are runnable and easy to follow.

### TC-01: Create profile
- Steps: Create a profile with a unique name.
- Expected: Profile is created with 0 gold and empty party.

### TC-02: Prevent duplicate profile
- Steps: Create same profile name twice.
- Expected: Second creation fails with validation error.

### TC-03: Create and add hero
- Steps: Add a hero to active party.
- Expected: Party size increases, hero appears with level 1 stats.

### TC-04: Inn visit revive/restore
- Steps: Damage hero until dead, visit inn.
- Expected: Hero revived and fully restored.

### TC-05: Item purchase with sufficient gold
- Steps: Add gold, buy Bread for hero.
- Expected: Gold reduced, hero HP increased.

### TC-06: Item purchase insufficient gold
- Steps: Try buying expensive item without enough gold.
- Expected: Purchase fails, gold unchanged.

### TC-07: Campaign room progression
- Steps: Start campaign, enter next room.
- Expected: Campaign room counter increments by 1.

### TC-08: Campaign completion
- Steps: Run through 30 rooms.
- Expected: Campaign ends, score is finalized.

### TC-09: PvP battle and league record
- Steps: Create two players, save parties, run PvP.
- Expected: Winner/loser recorded in league table unless draw.

### TC-10: Hall of Fame ordering
- Steps: Create profiles with different high scores.
- Expected: Hall of Fame returns descending by high score.

---

## Team Participation Plan

Use this format to satisfy rubric requirement for all members:

- Member 1: Runs first half of live demo (profile, campaign)
- Member 2: Runs second half of live demo (inn, PvP, league)
- Member 3: Presents architecture and module decomposition
- Member 4: Presents design patterns, tests, and conclusion

---

## Recording Checklist

- Resolution: at least 1080p
- Clear audio or subtitles
- Cursor zoom / large font size
- Keep demo under 5 minutes
- Keep design talk around 5 minutes
- Upload to YouTube as unlisted
- Submit shareable link on eClass

---

## Final Pre-Submission Checklist

- Tests pass (mvn clean test)
- Demo command works (java -cp target/classes app.DemoMain)
- Diagrams match current code packages/classes
- SDD references actual implemented methods
- Video has narration/subtitles
- All team members appear in the presentation
