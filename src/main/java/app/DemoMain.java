package app;

import controller.GameController;
import model.BattleResult;
import model.CampaignResult;
import model.Hero;
import model.HeroClass;
import model.InnItem;
import model.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * Console demo runner for video presentations.
 *
 * This class executes a deterministic walkthrough of major use cases so the team
 * can record a clean 3-5 minute application demo without building a UI first.
 */
public class DemoMain {

    public static void main(String[] args) {
        GameController game = new GameController();
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);

        String aliceName = "AliceDemo" + suffix;
        String bobName = "BobDemo" + suffix;

        System.out.println("=== Legends of Sword and Wand: Demo Walkthrough ===");

        System.out.println("\n[UC1] Create profile and heroes");
        Profile alice = game.createProfile(aliceName);
        Hero a1 = game.createHero("Astra", HeroClass.WARRIOR);
        game.createHero("Mira", HeroClass.ORDER);
        game.createHero("Zed", HeroClass.CHAOS);
        alice.saveParty(new ArrayList<>(alice.getActiveParty()));
        game.save();
        System.out.printf("Created profile %s with %d heroes.%n", aliceName, alice.getActiveParty().size());

        System.out.println("\n[UC6 + UC7] Campaign rooms and inn interaction");
        game.startCampaign();
        for (int i = 0; i < 3; i++) {
            CampaignResult room = game.enterNextRoom();
            System.out.printf("Room %d -> %s%n", i + 1, room.getRoomType());
        }

        if (a1 != null) {
            boolean bought = game.buyInnItem(InnItem.BREAD, a1);
            System.out.println("Bought BREAD for Astra: " + bought);
        }

        List<Hero> recruits = game.getRecruitableHeroes();
        if (!recruits.isEmpty()) {
            boolean recruited = game.recruitHero(recruits.get(0));
            System.out.println("Recruit attempt result: " + recruited);
        }
        game.save();

        System.out.println("\n[UC8] Create opponent profile and run PvP");
        Profile bob = game.createProfile(bobName);
        game.createHero("Rook", HeroClass.WARRIOR);
        game.createHero("Lyra", HeroClass.MAGE);
        bob.saveParty(new ArrayList<>(bob.getActiveParty()));
        game.save();

        BattleResult pvp = game.runPvpBattle(aliceName, 0, bobName, 0);
        if (pvp.isDraw()) {
            System.out.println("PvP result: Draw");
        } else {
            System.out.println("PvP result: Winner size = " + pvp.getWinningTeam().size());
        }

        System.out.println("\n[League] Current standings");
        game.getLeagueTable().forEach(System.out::println);

        System.out.println("\n[Hall of Fame]");
        game.getHallOfFame().forEach(p ->
                System.out.printf("%s | High Score: %d | Gold: %d%n",
                        p.getPlayerName(), p.getHighScore(), p.getGold()));

        System.out.println("\n=== Demo complete ===");
    }
}
