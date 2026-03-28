package app;

import controller.GameController;
import model.Ability;
import model.BattleResult;
import model.CampaignEncounter;
import model.CampaignResult;
import model.Hero;
import model.HeroClass;
import model.InnItem;
import model.LeagueEntry;
import model.Profile;
import service.impl.InnServiceImpl;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Playable Swing client for Legends of Sword and Wand.
 *
 * This UI intentionally keeps a text-based style while exposing every major
 * use case from the design document:
 * profile creation/loading, hero creation, campaign rooms, battles, inn
 * purchasing/recruitment, PvP, league table, and hall of fame.
 */
public class PlayDemoSwing {

    private static final int MANUAL_BATTLE_ROUND_LIMIT = 100;

    private final GameController game = new GameController();
    private final Random random = new Random();

    private JTextArea logArea;
    private JTextField profileNameField;
    private JTextField heroNameField;
    private JTextField opponentNameField;

    private JComboBox<HeroClass> heroClassCombo;
    private JComboBox<InnItem> itemCombo;
    private JComboBox<Hero> itemTargetCombo;
    private JComboBox<Hero> recruitCombo;

    private final List<Hero> currentRecruitCandidates = new ArrayList<>();
    private JPanel profilesPanel;
    private JPanel campaignPanel;
    private JPanel pvpPanel;

    // Action bar controls
    private JPanel actionBarPanel;
    private JButton idleEnterRoomBtn;
    private JButton idleVisitInnBtn;
    private JPanel battleActionsPanel;
    private JButton actAttackBtn;
    private JButton actDefendBtn;
    private JButton actCastBtn;
    private JButton actWaitBtn;
    private JButton actInspectBtn;

    // Battle interaction state (used when a campaign battle runs in interactive mode)
    private volatile boolean battleInProgress = false;
    private volatile Hero currentBattleActor = null;
    private volatile boolean playerAwaitingAction = false;
    private Queue<Hero> battleWaitQueue = new ConcurrentLinkedQueue<>();
    private List<Hero> battlePartySnapshot = new ArrayList<>();
    private List<Hero> battleEnemiesSnapshot = new ArrayList<>();
    // Inn UI pieces
    private JLabel innGoldLabel;
    // Shared shop/inventory UI
    private javax.swing.JList<InnItem> sharedItemList;
    private javax.swing.JTextArea sharedItemDescription;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PlayDemoSwing().createAndShow());
    }

    private void createAndShow() {
        JFrame frame = new JFrame("Legends of Sword and Wand - Playable Text RPG");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        logArea = new JTextArea(28, 110);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Adventure Log"));
        frame.add(scrollPane, BorderLayout.CENTER);

        // Build hidden panels (we will show PvP as a dialog instead of tabs)
        profilesPanel = buildProfilePanel();
        campaignPanel = buildCampaignPanel();
        pvpPanel = buildPvpPanel();

        // Action bar: context-sensitive controls (idle vs battle / post-signup)
        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.add(buildActionBarPanel(), BorderLayout.NORTH);
        frame.add(actionBar, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        appendLog("Welcome to Legends of Sword and Wand.");
        appendLog("Start by creating or loading a profile, then recruit heroes and begin your campaign.");
        // Show login / sign-up modal on startup
        SwingUtilities.invokeLater(this::showAuthDialog);
    }

    private JPanel buildProfilePanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 6, 6));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        profileNameField = new JTextField(14);
        JButton createProfileBtn = makeActionButton("Create Profile", 14, 140, 36);
        JButton loadProfileBtn = makeActionButton("Load Profile", 14, 140, 36);
        JButton statusBtn = makeActionButton("Show Status", 14, 140, 36);

        row1.add(new JLabel("Profile:"));
        row1.add(profileNameField);
        row1.add(createProfileBtn);
        row1.add(loadProfileBtn);
        row1.add(statusBtn);

        JPanel row2 = new JPanel(new BorderLayout(8, 8));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        heroNameField = new JTextField(12);
        heroClassCombo = new JComboBox<>(new HeroClass[]{
            HeroClass.ORDER, HeroClass.CHAOS, HeroClass.WARRIOR, HeroClass.MAGE
        });
        JButton createHeroBtn = makeActionButton("Create Hero", 14, 140, 36);
        JButton savePartyBtn = makeActionButton("Save Party Slot", 14, 140, 36);

        left.add(new JLabel("Hero Name:"));
        left.add(heroNameField);
        left.add(new JLabel("Class:"));
        left.add(heroClassCombo);
        left.add(createHeroBtn);
        left.add(savePartyBtn);

        // Right-side guide for hero classes
        JTextArea guideArea = new JTextArea();
        guideArea.setEditable(false);
        guideArea.setLineWrap(true);
        guideArea.setWrapStyleWord(true);
        guideArea.setText(
            "Class Guide:\n\n" +
                "ORDER — Support: better defense and mana growth. Abilities: Protect, Heal.\n" +
                "  Stat bonuses: +def growth, +max mana. Good as primary healer/support.\n\n" +
                "CHAOS — Durable damage: strong attack growth and bonus max HP. Abilities: Fireball, ChainLightning.\n" +
                "  Stat bonuses: +atk growth, +max HP. Good as frontline caster/damage.\n\n" +
                "WARRIOR — Melee fighter: balanced attack and defense growth. Ability: BerserkerAttack.\n" +
                "  Stat bonuses: +atk growth, +def growth. Good for physical DPS/tank.\n\n" +
                "MAGE — Spellcaster: more mana and spell attack. Abilities: Replenish.\n"
        );

        JScrollPane guideScroll = new JScrollPane(guideArea);
        guideScroll.setBorder(BorderFactory.createTitledBorder("Class Guide"));
        guideScroll.setPreferredSize(new java.awt.Dimension(320, 120));

        row2.add(left, BorderLayout.CENTER);
        row2.add(guideScroll, BorderLayout.EAST);

        createProfileBtn.addActionListener(e -> runUiAction(this::showAuthDialog));
        loadProfileBtn.addActionListener(e -> runUiAction(this::showAuthDialog));
        createHeroBtn.addActionListener(e -> runUiAction(this::createHero));
        savePartyBtn.addActionListener(e -> runUiAction(this::savePartySlot));
        statusBtn.addActionListener(e -> runUiAction(this::showStatus));

        panel.add(row1);
        panel.add(row2);
        return panel;
    }

    private JPanel buildCampaignPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 6, 6));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton startCampaignBtn = makeActionButton("Start Campaign", 16, 160, 44);
        JButton nextRoomBtn = makeActionButton("Enter Next Room", 16, 160, 44);
        JButton visitInnBtn = makeActionButton("Visit Inn", 16, 160, 44);

        row1.add(startCampaignBtn);
        row1.add(nextRoomBtn);
        row1.add(visitInnBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        itemCombo = new JComboBox<>(InnItem.values());
        itemTargetCombo = new JComboBox<>();
        recruitCombo = new JComboBox<>();
        recruitCombo.setRenderer(new RecruitRenderer());

        // Purchasing is only allowed in the Inn Shop now; remove campaign buy button.
        JButton refreshRecruitsBtn = makeActionButton("Refresh Recruits", 14, 140, 36);
        JButton recruitBtn = makeActionButton("Recruit Hero", 14, 140, 36);

        row2.add(new JLabel("Item:"));
        row2.add(itemCombo);
        row2.add(new JLabel("Target:"));
        row2.add(itemTargetCombo);
        row2.add(refreshRecruitsBtn);
        row2.add(recruitCombo);
        row2.add(recruitBtn);

        startCampaignBtn.addActionListener(e -> runUiAction(this::startCampaign));
        nextRoomBtn.addActionListener(e -> runUiAction(this::enterNextRoom));
        visitInnBtn.addActionListener(e -> runUiAction(this::visitInn));
        // buyItemBtn removed; purchases must be done in the Inn Shop.
        refreshRecruitsBtn.addActionListener(e -> runUiAction(this::refreshRecruits));
        recruitBtn.addActionListener(e -> runUiAction(this::recruitSelectedHero));

        panel.add(row1);
        panel.add(row2);
        return panel;
    }

    private JPanel buildPvpPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        opponentNameField = new JTextField(14);
        JButton createOpponentAndBattleBtn = makeActionButton("Search", 14, 180, 40);
        JButton showLeagueBtn = makeActionButton("Show League Table", 14, 160, 40);
        JButton showHallOfFameBtn = makeActionButton("Show Hall of Fame", 14, 160, 40);

        panel.add(new JLabel("Opponent:"));
        panel.add(opponentNameField);
        panel.add(createOpponentAndBattleBtn);
        panel.add(showLeagueBtn);
        panel.add(showHallOfFameBtn);

        createOpponentAndBattleBtn.addActionListener(e -> runUiAction(this::createOpponentAndRunPvp));
        showLeagueBtn.addActionListener(e -> runUiAction(this::showLeagueTable));
        showHallOfFameBtn.addActionListener(e -> runUiAction(this::showHallOfFame));

        return panel;
    }

    private JPanel buildActionBarPanel() {
        actionBarPanel = new JPanel(new BorderLayout());

        // Idle buttons (consistent size)
        idleEnterRoomBtn = makeActionButton("Enter Room", 20, 220, 72);
        idleVisitInnBtn = makeActionButton("Inventory", 20, 220, 72);
        idleEnterRoomBtn.addActionListener(e -> runUiAction(this::enterNextRoom));
        // Inventory opens a dialog to use/buy items for party heroes
        idleVisitInnBtn.addActionListener(e -> runUiAction(this::showInventoryDialog));

        // Battle action buttons (visual; core battle loop still uses dialogs)
        battleActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actAttackBtn = makeActionButton("Attack", 18, 180, 64);
        actDefendBtn = makeActionButton("Defend", 18, 180, 64);
        actCastBtn = makeActionButton("Cast", 18, 180, 64);
        actWaitBtn = makeActionButton("Wait", 18, 180, 64);
        actInspectBtn = makeActionButton("Inspect", 18, 180, 64);

        // Action bar buttons drive the current player's turn when a battle is active.
        actAttackBtn.addActionListener(e -> performBattleAction("Attack"));
        actDefendBtn.addActionListener(e -> performBattleAction("Defend"));
        actCastBtn.addActionListener(e -> performBattleAction("Cast"));
        actWaitBtn.addActionListener(e -> performBattleAction("Wait"));
        actInspectBtn.addActionListener(e -> performBattleAction("Inspect"));

        battleActionsPanel.add(actAttackBtn);
        battleActionsPanel.add(actDefendBtn);
        battleActionsPanel.add(actCastBtn);
        battleActionsPanel.add(actWaitBtn);
        battleActionsPanel.add(actInspectBtn);

        // Start in idle mode
        setActionBarIdle();
        return actionBarPanel;
    }

    private void setActionBarIdle() {
        actionBarPanel.removeAll();
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(idleEnterRoomBtn);
        p.add(idleVisitInnBtn);
        actionBarPanel.add(p, BorderLayout.WEST);
        // Right-side small controls: show status, exit campaign, logout (or login)
        // Stack Exit above Logout vertically
        JPanel right = new JPanel(new GridLayout(0,1,4,4));
        if (game.getCurrentProfile() == null) {
            JButton loginBtn = makeActionButton("Log In", 12, 100, 28);
            loginBtn.addActionListener(e -> runUiAction(this::showAuthDialog));
            right.add(loginBtn);
        } else {
            JButton showStatusSmall = makeActionButton("Status", 12, 100, 28);
            showStatusSmall.addActionListener(e -> runUiAction(this::showStatus));
            right.add(showStatusSmall);

            JButton exitSmall = makeActionButton("Exit", 12, 100, 28);
            exitSmall.addActionListener(e -> runUiAction(() -> {
                try {
                    game.exitCampaign();
                    appendLog("Exited campaign and saved profile.");
                    // After exiting, show the post-signup menu so user can Resume or Start
                    SwingUtilities.invokeLater(this::setActionBarPostSignup);
                } catch (IllegalStateException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Exit Failed", JOptionPane.WARNING_MESSAGE);
                }
            }));
            // Only enable exit if a campaign is active
            exitSmall.setEnabled(game.getCurrentProfile() != null && game.getCurrentProfile().isCampaignActive());
            right.add(exitSmall);

            JButton logoutSmall = makeActionButton("Log Out", 12, 100, 28);
            logoutSmall.addActionListener(e -> {
                try { game.save(); } catch (Exception ignored) {}
                try { if (game != null) game.logout(); } catch (Exception ignored) {}
                appendLog("Logged out.");
                setActionBarIdle();
                SwingUtilities.invokeLater(this::showAuthDialog);
            });
            right.add(logoutSmall);
        }
        actionBarPanel.add(right, BorderLayout.EAST);
        actionBarPanel.revalidate();
        actionBarPanel.repaint();
    }

    private void setActionBarBattle() {
        actionBarPanel.removeAll();
        actionBarPanel.add(battleActionsPanel, BorderLayout.WEST);
        // Right-side: stack Exit above Logout for post-battle controls
        JPanel right = new JPanel(new GridLayout(0,1,4,4));
        if (game.getCurrentProfile() == null) {
            JButton loginBtn = makeActionButton("Log In", 12, 100, 28);
            loginBtn.addActionListener(e -> runUiAction(this::showAuthDialog));
            right.add(loginBtn);
        } else {
            JButton exitSmall = makeActionButton("Exit", 12, 100, 28);
            exitSmall.addActionListener(e -> runUiAction(() -> {
                try {
                    game.exitCampaign();
                    appendLog("Exited campaign and saved profile.");
                    SwingUtilities.invokeLater(this::setActionBarPostSignup);
                } catch (IllegalStateException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Exit Failed", JOptionPane.WARNING_MESSAGE);
                }
            }));
            exitSmall.setEnabled(game.getCurrentProfile() != null && game.getCurrentProfile().isCampaignActive());
            right.add(exitSmall);

            JButton logoutSmall = makeActionButton("Log Out", 12, 100, 28);
            logoutSmall.addActionListener(e -> {
                try { game.save(); } catch (Exception ignored) {}
                try { if (game != null) game.logout(); } catch (Exception ignored) {}
                appendLog("Logged out.");
                setActionBarIdle();
                SwingUtilities.invokeLater(this::showAuthDialog);
            });
            right.add(logoutSmall);
        }
        actionBarPanel.add(right, BorderLayout.EAST);
        actionBarPanel.revalidate();
        actionBarPanel.repaint();
    }

    private void performBattleAction(String action) {
        if (!battleInProgress || currentBattleActor == null || !currentBattleActor.isAlive()) {
            appendLog("No active player turn to perform action.");
            return;
        }

        // Only allow player-controlled heroes to be driven by the action bar.
        // Accept heroes that are either in the active profile's party (campaign)
        // or present in the current battle snapshot (interactive PvP uses deep copies).
        Profile current = game.getCurrentProfile();
        boolean isPlayerHero = (current != null && current.getActiveParty().contains(currentBattleActor))
                || battlePartySnapshot.contains(currentBattleActor);
        if (!isPlayerHero) {
            appendLog("Action bar controls only apply to player heroes.");
            return;
        }

        try {
            switch (action) {
                case "Attack" -> {
                    List<Hero> alive = aliveMembers(battleEnemiesSnapshot);
                    if (alive.isEmpty()) {
                        appendLog("No enemies available to attack.");
                        return;
                    }
                    String[] labels = new String[alive.size()];
                    for (int i = 0; i < alive.size(); i++) {
                        Hero h = alive.get(i);
                        labels[i] = h.getName() + " (" + h.getCurrentHealth() + " HP, " + h.getCurrentMana() + " MP)";
                    }

                    Object sel = JOptionPane.showInputDialog(
                            null,
                            "Choose attack target for " + currentBattleActor.getName() + ":",
                            "Choose Target",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            labels,
                            labels[0]
                    );

                    if (sel == null) {
                        appendLog("Attack cancelled.");
                        // do not clear turn; let player choose again
                        return;
                    }

                    int idx = 0;
                    for (int i = 0; i < labels.length; i++) {
                        if (labels[i].equals(sel)) {
                            idx = i;
                            break;
                        }
                    }
                    Hero target = alive.get(idx);
                    int dmg = currentBattleActor.attack(target);
                    appendLog(currentBattleActor.getName() + " attacks " + target.getName() + " for " + dmg + " damage. (ATK "
                            + currentBattleActor.getCurrentAttack() + " vs DEF " + target.getCurrentDefense() + ")");
                    // turn complete
                    playerAwaitingAction = false;
                    currentBattleActor = null;
                }
                case "Defend" -> {
                    currentBattleActor.defend();
                    appendLog(currentBattleActor.getName() + " defends and recovers HP/mana.");
                    playerAwaitingAction = false;
                    currentBattleActor = null;
                }
                case "Cast" -> {
                    List<Ability> castable = currentBattleActor.getClassAbilities().stream().filter(currentBattleActor::canCast).toList();
                    if (castable.isEmpty()) {
                        appendLog(currentBattleActor.getName() + " has no castable abilities and defaults to attack.");
                        List<Hero> alive = aliveMembers(battleEnemiesSnapshot);
                        Hero fallback = alive.isEmpty() ? null : alive.get(0);
                        if (fallback != null) {
                            int dmg = currentBattleActor.attack(fallback);
                            appendLog(currentBattleActor.getName() + " attacks " + fallback.getName() + " for " + dmg + " damage.");
                        }
                        playerAwaitingAction = false;
                        currentBattleActor = null;
                    } else {
                        // Build dialog to let player pick ability and target
                        JPanel panel = new JPanel(new BorderLayout(8,8));
                        DefaultComboBoxModel<Ability> abilityModel = new DefaultComboBoxModel<>();
                        for (Ability a : castable) abilityModel.addElement(a);
                        JComboBox<Ability> abilityBox = new JComboBox<>(abilityModel);
                        abilityBox.setRenderer(new DefaultListCellRenderer() {
                            @Override
                            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                                if (value instanceof Ability ab) setText(ab.getName() + " (" + ab.getManaCost() + " MP)");
                                return this;
                            }
                        });

                        JTextArea abilityDesc = new JTextArea(6,40);
                        abilityDesc.setEditable(false);
                        abilityDesc.setLineWrap(true);
                        abilityDesc.setWrapStyleWord(true);

                        JComboBox<Hero> targetBox = new JComboBox<>();
                        targetBox.setRenderer(new DefaultListCellRenderer() {
                            @Override
                            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                                if (value instanceof Hero h) setText(h.getName() + " (HP " + h.getCurrentHealth() + ")");
                                return this;
                            }
                        });

                        Runnable updateTargetsAndDesc = () -> {
                            Ability sel = (Ability) abilityBox.getSelectedItem();
                            if (sel == null) {
                                abilityDesc.setText("");
                                targetBox.removeAllItems();
                                return;
                            }
                            StringBuilder sb = new StringBuilder();
                            sb.append(sel.getName()).append(" — Cost: ").append(sel.getManaCost()).append(" MP\n");
                            // Brief descriptions for known abilities
                            sb.append("Description: ");
                            switch (sel.getName()) {
                                case "Protect": sb.append("Shields all allies for 10% max HP."); break;
                                case "Heal": sb.append("Heals lowest-HP ally for 25% of their max HP."); break;
                                case "Replenish": sb.append("Restores 30 mana to allies and 60 to caster."); break;
                                case "Fireball": sb.append("Hits up to 3 enemies; primary target chosen."); break;
                                case "Chain Lightning": sb.append("Hits all enemies, damage cascades from primary target."); break;
                                case "Berserker Attack": sb.append("Primary full damage, plus splash damage to 2 other targets."); break;
                                default: sb.append("Special ability.");
                            }
                            abilityDesc.setText(sb.toString());

                            // Populate targets based on support vs enemy ability
                            targetBox.removeAllItems();
                            List<Hero> targets = isSupportAbility(sel) ? aliveMembers(battlePartySnapshot) : aliveMembers(battleEnemiesSnapshot);
                            for (Hero h : targets) targetBox.addItem(h);
                        };

                        abilityBox.addActionListener(e -> updateTargetsAndDesc.run());
                        abilityBox.setSelectedIndex(0);
                        updateTargetsAndDesc.run();

                        JPanel top = new JPanel(new BorderLayout(6,6));
                        top.add(new JLabel("Ability:"), BorderLayout.WEST);
                        top.add(abilityBox, BorderLayout.CENTER);

                        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
                        bottom.add(new JLabel("Target:"));
                        bottom.add(targetBox);

                        panel.add(top, BorderLayout.NORTH);
                        panel.add(new JScrollPane(abilityDesc), BorderLayout.CENTER);
                        panel.add(bottom, BorderLayout.SOUTH);

                        int res = JOptionPane.showConfirmDialog(null, panel, "Cast Ability", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                        if (res == JOptionPane.OK_OPTION) {
                            Ability chosen = (Ability) abilityBox.getSelectedItem();
                            Hero tgt = (Hero) targetBox.getSelectedItem();
                            if (chosen != null) {
                                List<Hero> targets = new ArrayList<>();
                                if (isSupportAbility(chosen)) {
                                    targets.addAll(aliveMembers(battlePartySnapshot));
                                } else {
                                    if (tgt != null) {
                                        // primary target first, then the rest
                                        targets.add(tgt);
                                        for (Hero h : aliveMembers(battleEnemiesSnapshot)) if (h != tgt) targets.add(h);
                                    } else {
                                        targets.addAll(aliveMembers(battleEnemiesSnapshot));
                                    }
                                }
                                currentBattleActor.castAbility(chosen, targets);
                                appendLog(currentBattleActor.getName() + " casts " + chosen.getName() + ".");
                            }
                        } else {
                            appendLog("Cast cancelled.");
                            // don't consume turn
                            return;
                        }

                        playerAwaitingAction = false;
                        currentBattleActor = null;
                    }
                }
                case "Wait" -> {
                    currentBattleActor.waitTurn();
                    battleWaitQueue.add(currentBattleActor);
                    appendLog(currentBattleActor.getName() + " waits and will act at end of round.");
                    playerAwaitingAction = false;
                    currentBattleActor = null;
                }
                case "Inspect" -> {
                    appendLog("Inspect -> Party: " + summarizeTeam(battlePartySnapshot));
                    appendLog("Inspect -> Enemies: " + summarizeTeam(battleEnemiesSnapshot));
                    // do not clear turn; allow player to pick another action
                    return;
                }
                default -> appendLog("Unknown action: " + action);
            }
        } catch (Exception ex) {
            appendLog("Action failed: " + ex.getMessage());
        }
    }

    private void setActionBarPostSignup() {
        actionBarPanel.removeAll();

        JPanel center = new JPanel();
        center.setLayout(new java.awt.GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(8, 40, 8, 40);

        JButton pvpBig = makeActionButton("PvP/Records", 28, 280, 80);
        pvpBig.addActionListener(e -> SwingUtilities.invokeLater(this::showPvpDialog));

        Profile cur = game.getCurrentProfile();
        // If the current profile has saved campaign progress (paused/previous run),
        // offer a Resume Campaign button instead of starting a new run.
        if (cur != null && cur.getCampaignRoom() > 0) {
            JButton resumeBig = makeActionButton("Resume Campaign", 28, 280, 80);
            resumeBig.addActionListener(e -> runUiAction(() -> {
                try {
                    game.resumeCampaign();
                    appendLog("Resumed campaign.");
                    SwingUtilities.invokeLater(this::setActionBarCampaign);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Resume Failed", JOptionPane.WARNING_MESSAGE);
                }
            }));

            gbc.gridx = 0; gbc.gridy = 0;
            center.add(resumeBig, gbc);
            gbc.gridx = 1; gbc.gridy = 0;
            center.add(pvpBig, gbc);
        } else {
            JButton startCampaignBig = makeActionButton("Start Campaign", 28, 280, 80);
            startCampaignBig.addActionListener(e -> runUiAction(this::startCampaign));

            gbc.gridx = 0; gbc.gridy = 0;
            center.add(startCampaignBig, gbc);
            gbc.gridx = 1; gbc.gridy = 0;
            center.add(pvpBig, gbc);
        }

        actionBarPanel.add(center, BorderLayout.CENTER);

        JButton logout = makeActionButton("Log Out", 14, 120, 36);
        logout.addActionListener(e -> {
            try {
                game.save();
            } catch (Exception ignored) {}
            try { if (game != null) game.logout(); } catch (Exception ignored) {}
            appendLog("Logged out.");
            // revert to idle action bar and show auth
            setActionBarIdle();
            SwingUtilities.invokeLater(this::showAuthDialog);
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // Show login button when no profile is active (helps when user dismissed auth)
        if (game.getCurrentProfile() == null) {
            JButton loginBtn = makeActionButton("Log In", 12, 100, 28);
            loginBtn.addActionListener(e -> runUiAction(this::showAuthDialog));
            right.add(loginBtn);
        }
        right.add(logout);
        actionBarPanel.add(right, BorderLayout.EAST);

        actionBarPanel.revalidate();
        actionBarPanel.repaint();
    }

    private void showPvpDialog() {
        if (pvpPanel == null) pvpPanel = buildPvpPanel();
        JOptionPane.showMessageDialog(null, pvpPanel, "PvP / Records", JOptionPane.PLAIN_MESSAGE);
    }

    /** Factory for consistently-styled action buttons. */
    private JButton makeActionButton(String label, int fontSize, int width, int height) {
        JButton b = new JButton(label);
        b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
        b.setPreferredSize(new java.awt.Dimension(width, height));
        return b;
    }

    private void showInventoryDialog() {
        ensureProfileSelected();
        // Use the shared item list + preview panel for inventory as well
        JPanel panel = new JPanel(new BorderLayout(8,8));

        JPanel itemPanel = buildItemListPreviewPanel("Items");
        panel.add(itemPanel, BorderLayout.CENTER);

        // Render sharedItemList with counts for inventory view
        sharedItemList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof InnItem) {
                    InnItem it = (InnItem) value;
                    Profile cur = game.getCurrentProfile();
                    int cnt = cur == null ? 0 : cur.getInventoryCount(it);
                    setText(it.getDisplayName() + " x" + cnt);
                }
                return this;
            }
        });

        if (itemTargetCombo == null) {
            itemTargetCombo = new JComboBox<>();
            itemTargetCombo.setRenderer(new HeroRenderer());
        }
        // Refresh targets to current active party
        refreshPartySelectors();

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton useBtn = makeActionButton("Use", 14, 120, 36);
        useBtn.addActionListener(e -> runUiAction(this::useSelectedInventoryItem));
        bottom.add(new JLabel("Target:"));
        bottom.add(itemTargetCombo);
        bottom.add(useBtn);

        panel.add(bottom, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(null, panel, "Inventory", JOptionPane.PLAIN_MESSAGE);
    }

    /** Shop dialog used when inside an inn. Shows items and previews stat boosts. */
    private void showShopDialog() {
        ensureProfileSelected();

        
            JPanel panel = new JPanel(new BorderLayout(8,8));

            JPanel itemPanel = buildItemListPreviewPanel("Shop Items");
            panel.add(itemPanel, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton buyBtn = makeActionButton("Buy", 14, 140, 36);
            bottom.add(buyBtn);
            panel.add(bottom, BorderLayout.SOUTH);

            JLabel goldLabel = new JLabel();
            panel.add(goldLabel, BorderLayout.NORTH);

            Runnable updateGold = () -> {
                Profile cur = game.getCurrentProfile();
                goldLabel.setText("Gold: " + (cur == null ? 0 : cur.getGold()));
                updateInnGoldLabel();
            };
            updateGold.run();

            buyBtn.addActionListener(ae -> {
                InnItem sel = sharedItemList == null ? null : sharedItemList.getSelectedValue();
                if (sel == null) {
                    JOptionPane.showMessageDialog(null, "Select an item to buy.", "Buy Failed", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                boolean ok = game.buyInnItem(sel, null);
                if (ok) {
                    appendLog("Purchased " + sel.getDisplayName() + " (added to inventory).");
                    updateGold.run();
                } else {
                    JOptionPane.showMessageDialog(null, "Insufficient gold.", "Buy Failed", JOptionPane.WARNING_MESSAGE);
                }
            });

            JOptionPane.showMessageDialog(null, panel, "Inn Shop", JOptionPane.PLAIN_MESSAGE);
        }

    private void showRecruitDialog() {
        ensureProfileSelected();

        JPanel panel = new JPanel(new BorderLayout(8,8));

        // Refresh candidate list first
        refreshRecruits();

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT));
        center.add(new JLabel("Candidates:"));
        if (recruitCombo == null) recruitCombo = new JComboBox<>();
        center.add(recruitCombo);

        JTextArea details = new JTextArea(6, 40);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);

        recruitCombo.addActionListener(e -> {
            Hero sel = (Hero) recruitCombo.getSelectedItem();
            if (sel == null) {
                details.setText("");
                return;
            }
            int cost = InnServiceImpl.recruitmentCost(sel.getLevel());
            StringBuilder sb = new StringBuilder();
            sb.append(sel.getName()).append(" — Level ").append(sel.getLevel()).append(" ").append(sel.getHeroClass()).append("\n");
            sb.append("Cost: ").append(cost).append(" gold\n\n");
            sb.append("Stats: HP ").append(sel.getCurrentMaxHealth()).append(", MP ").append(sel.getCurrentMaxMana()).append(", ATK ").append(sel.getCurrentAttack()).append(", DEF ").append(sel.getCurrentDefense());
            details.setText(sb.toString());
        });

        JScrollPane descScroll = new JScrollPane(details);
        descScroll.setBorder(BorderFactory.createTitledBorder("Candidate Details"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = makeActionButton("Refresh", 14, 120, 36);
        refreshBtn.addActionListener(ae -> runUiAction(this::refreshRecruits));
        JButton recruitNow = makeActionButton("Recruit", 14, 140, 36);
        recruitNow.addActionListener(ae -> runUiAction(this::recruitSelectedHero));
        bottom.add(refreshBtn);
        bottom.add(recruitNow);

        panel.add(center, BorderLayout.NORTH);
        panel.add(descScroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(null, panel, "Inn Recruitment", JOptionPane.PLAIN_MESSAGE);
    }

    /** Build or return a shared item list + preview panel for shop/inventory UIs. */
        private JPanel buildItemListPreviewPanel(String title) {
            if (sharedItemList == null) {
                DefaultListModel<InnItem> listModel = new DefaultListModel<>();
                for (InnItem it : InnItem.values()) listModel.addElement(it);
                sharedItemList = new javax.swing.JList<>(listModel);
                sharedItemList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

                sharedItemDescription = new javax.swing.JTextArea(6, 40);
                sharedItemDescription.setEditable(false);
                sharedItemDescription.setLineWrap(true);
                sharedItemDescription.setWrapStyleWord(true);

                sharedItemList.addListSelectionListener(e -> {
                    InnItem sel = sharedItemList.getSelectedValue();
                    if (sel == null) {
                        sharedItemDescription.setText("");
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(sel.getDisplayName()).append(" — cost: ").append(sel.getCost()).append("g\n");
                    if (sel.getHpRestore() == Integer.MAX_VALUE) sb.append("Revives and fully restores HP/MP.\n");
                    else if (sel.getHpRestore() > 0) sb.append("Heals: +").append(sel.getHpRestore()).append(" HP\n");
                    if (sel.getManaRestore() == Integer.MAX_VALUE) sb.append("Full mana restore.\n");
                    else if (sel.getManaRestore() > 0) sb.append("Restores: +").append(sel.getManaRestore()).append(" MP\n");
                    sharedItemDescription.setText(sb.toString());
                });
            }

            JScrollPane listScroll = new JScrollPane(sharedItemList);
            listScroll.setBorder(BorderFactory.createTitledBorder(title));
            listScroll.setPreferredSize(new java.awt.Dimension(300, 180));

            JScrollPane descScroll = new JScrollPane(sharedItemDescription);
            descScroll.setBorder(BorderFactory.createTitledBorder("Item Details"));

            JPanel right = new JPanel(new BorderLayout(6,6));
            right.add(descScroll, BorderLayout.CENTER);

            JPanel panel = new JPanel(new BorderLayout(8,8));
            panel.add(listScroll, BorderLayout.WEST);
            panel.add(right, BorderLayout.CENTER);
            return panel;
        }

        private void updateInnGoldLabel() {
            if (innGoldLabel == null) return;
            Profile cur = game.getCurrentProfile();
            innGoldLabel.setText(cur == null ? "Gold: 0" : "Gold: " + cur.getGold());
        }

        private void runUiAction(Runnable action) {
            try {
                action.run();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                appendLog("ERROR: " + ex.getMessage());
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Action Failed", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                appendLog("UNEXPECTED ERROR: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
                JOptionPane.showMessageDialog(null, ex.toString(), "Unexpected Error", JOptionPane.ERROR_MESSAGE);
            }
        }

    private void createProfile() {
        String name = clean(profileNameField.getText());
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Enter a profile name first.");
        }
        String pw = JOptionPane.showInputDialog(null, "Set a password (leave blank for no password):", "Set Password", JOptionPane.QUESTION_MESSAGE);
        Profile profile = game.createProfile(name, pw == null ? "" : pw);
        appendLog("Created profile: " + profile.getPlayerName());
        refreshPartySelectors();
        showStatus();
        // Helpful class guide for new players
        appendLog("Class Guide:");
        appendLog("ORDER — Support: +mana growth, +def; abilities: Protect, Heal.");
        appendLog("CHAOS — Tank/Damage: +atk growth, +max HP; abilities: Fireball, ChainLightning.");
        appendLog("WARRIOR — Melee: +atk and +def growth; ability: BerserkerAttack.");
        appendLog("MAGE — Caster: +mana and +spell attack growth; ability: Replenish.");
        appendLog("Tip: ORDER = healer/support, CHAOS = durable damage, WARRIOR = physical DPS, MAGE = spellcaster.");
        // Post-creation flows for a brand-new profile (action bar + first-hero recruit)
        handleNewProfile(profile);
    }

    private void loadProfile() {
        String name = clean(profileNameField.getText());
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Enter a profile name to load.");
        }
        String pw = JOptionPane.showInputDialog(null, "Enter password (leave blank if none):", "Profile Password", JOptionPane.QUESTION_MESSAGE);
        Profile profile = game.loadProfile(name, pw == null ? "" : pw);
        if (profile == null) {
            appendLog("No profile found with name or password mismatch: " + name);
            return;
        }

        appendLog("Loaded profile: " + profile.getPlayerName());
        refreshPartySelectors();
        refreshRecruits();
        showStatus();
    }

    private void showAuthDialog() {
        // Loop until user successfully logs in/signs up, or cancels.
        while (true) {
            JPanel panel = new JPanel(new GridLayout(3, 2, 6, 6));
            JTextField nameField = new JTextField(14);
            JPasswordField pwField = new JPasswordField(14);
            JComboBox<String> mode = new JComboBox<>(new String[]{"Login", "Sign Up"});

            panel.add(new JLabel("Mode:"));
            panel.add(mode);
            panel.add(new JLabel("Player name:"));
            panel.add(nameField);
            panel.add(new JLabel("Password:"));
            panel.add(pwField);

            int res = JOptionPane.showConfirmDialog(null, panel, "Login / Sign Up",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return; // user cancelled

            String player = clean(nameField.getText());
            String password = new String(pwField.getPassword());
            if (player.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Enter a profile name.", "Missing Name", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            String chosen = (String) mode.getSelectedItem();
            try {
                if ("Sign Up".equals(chosen)) {
                    Profile profile = game.createProfile(player, password == null ? "" : password);
                    appendLog("Created profile: " + profile.getPlayerName());
                    refreshPartySelectors();
                    showStatus();
                    // centralize post-new-profile behavior (action bar + recruit)
                    handleNewProfile(profile);
                    break;
                } else {
                    Profile profile = game.loadProfile(player, password == null ? "" : password);
                    if (profile == null) {
                        JOptionPane.showMessageDialog(null, "Login failed (not found or password mismatch).", "Login Failed", JOptionPane.ERROR_MESSAGE);
                        // loop again
                        continue;
                    }
                    appendLog("Loaded profile: " + profile.getPlayerName());
                    refreshPartySelectors();
                    refreshRecruits();
                    showStatus();
                    break;
                }
            } catch (IllegalArgumentException ex) {
                // e.g., sign up name already exists
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
                // loop again
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // After successful auth, ensure the action bar reflects signed-in state.
        try {
            Profile current = game.getCurrentProfile();
            if (current != null) {
                SwingUtilities.invokeLater(this::setActionBarPostSignup);
            }
        } catch (Exception ignored) {}
    }

    private void showFirstHeroRecruitDialog() {
        while (true) {
            JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
            JTextField nameField = new JTextField(14);
            JComboBox<HeroClass> classBox = new JComboBox<>(new HeroClass[]{
                    HeroClass.ORDER, HeroClass.CHAOS, HeroClass.WARRIOR, HeroClass.MAGE
            });

            form.add(new JLabel("Hero name:"));
            form.add(nameField);
            form.add(new JLabel("Class:"));
            form.add(classBox);

            JTextArea guide = new JTextArea();
            guide.setEditable(false);
            guide.setLineWrap(true);
            guide.setWrapStyleWord(true);
            guide.setText(
                    "Class Guide:\n\n" +
                            "ORDER — Support: better defense and mana growth. Abilities: Protect, Heal.\n" +
                            "  Stat bonuses: +def growth, +max mana. Good as primary healer/support.\n\n" +
                            "CHAOS — Durable damage: strong attack growth and bonus max HP. Abilities: Fireball, ChainLightning.\n" +
                            "  Stat bonuses: +atk growth, +max HP. Good as frontline caster/damage.\n\n" +
                            "WARRIOR — Melee fighter: balanced attack and defense growth. Ability: BerserkerAttack.\n" +
                            "  Stat bonuses: +atk growth, +def growth. Good for physical DPS/tank.\n\n" +
                            "MAGE — Spellcaster: more mana and spell attack. Abilities: Replenish.\n" +
                            "  Stat bonuses: +mana growth, +spell power. Good for burst magic damage.");

            JScrollPane guideScroll = new JScrollPane(guide);
            guideScroll.setBorder(BorderFactory.createTitledBorder("Class Guide"));
            guideScroll.setPreferredSize(new java.awt.Dimension(320, 160));

            JPanel container = new JPanel(new BorderLayout(8, 8));
            container.add(form, BorderLayout.CENTER);
            container.add(guideScroll, BorderLayout.EAST);

            int res = JOptionPane.showConfirmDialog(null, container, "Recruit your first hero",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;

            String heroName = clean(nameField.getText());
            if (heroName.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Enter a hero name.", "Missing Name", JOptionPane.WARNING_MESSAGE);
                // loop again to let user enter a name or cancel
                continue;
            }

            HeroClass chosen = (HeroClass) classBox.getSelectedItem();

            try {
                Hero created = game.createHero(heroName, chosen);
                if (created != null) {
                    appendLog("Recruited: " + formatHero(created));
                    refreshPartySelectors();
                    showStatus();
                } else {
                    appendLog("Failed to recruit hero (party may be full). Please try again.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Could not recruit hero: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
    }

    /**
     * Handle flows that should run when a brand-new profile is created:
     * - switch the action bar to the post-signup layout
     * - if the profile has no heroes, open the recruit-first-hero dialog
     */
    private void handleNewProfile(Profile profile) {
        if (profile == null) return;
        SwingUtilities.invokeLater(this::setActionBarPostSignup);
        if (profile.getActiveParty().isEmpty()) {
            SwingUtilities.invokeLater(this::showFirstHeroRecruitDialog);
        }
    }

    private void createHero() {
        ensureProfileSelected();

        String heroName = clean(heroNameField.getText());
        if (heroName.isEmpty()) {
            throw new IllegalArgumentException("Enter a hero name first.");
        }

        HeroClass selectedClass = (HeroClass) heroClassCombo.getSelectedItem();
        Hero hero = game.createHero(heroName, selectedClass);

        if (hero == null) {
            appendLog("Party is full (max 5 heroes).");
            return;
        }

        appendLog("Hero created: " + formatHero(hero));
        heroNameField.setText("");
        refreshPartySelectors();
        showStatus();
    }

    private void startCampaign() {
        ensureProfileSelected();
        game.startCampaign();
        appendLog("Campaign started. 30 rooms total.");
        appendLog("Narrator: You enter the first corridor of the Sword-and-Wand labyrinth.");
        appendLog("Narrator: Each room may hide enemies or shelter an inn.");
        refreshRecruits();
        // Switch the action bar into campaign mode (large Enter Room / Inventory buttons)
        SwingUtilities.invokeLater(this::setActionBarCampaign);
    }

    /**
     * Action bar layout while a campaign is active: two large centered buttons
     * for entering rooms and opening the inventory, plus small status/logout
     * controls.
     */
    private void setActionBarCampaign() {
        actionBarPanel.removeAll();

        JPanel center = new JPanel();
        center.setLayout(new java.awt.GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(8, 40, 8, 40);

        JButton enterRoomBig = makeActionButton("Enter Room", 28, 280, 80);
        enterRoomBig.addActionListener(e -> runUiAction(this::enterNextRoom));

        JButton inventoryBig = makeActionButton("Inventory", 28, 280, 80);
        inventoryBig.addActionListener(e -> runUiAction(this::showInventoryDialog));

        gbc.gridx = 0; gbc.gridy = 0;
        center.add(enterRoomBig, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        center.add(inventoryBig, gbc);

        actionBarPanel.add(center, BorderLayout.CENTER);

        JButton showStatusBtn = makeActionButton("Show Status", 14, 140, 36);
        showStatusBtn.addActionListener(e -> runUiAction(this::showStatus));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(showStatusBtn);
        actionBarPanel.add(left, BorderLayout.WEST);

        JButton logout = makeActionButton("Log Out", 14, 120, 36);
        logout.addActionListener(e -> {
            try { game.save(); } catch (Exception ignored) {}
            try { if (game != null) game.logout(); } catch (Exception ignored) {}
            appendLog("Logged out.");
            setActionBarIdle();
            SwingUtilities.invokeLater(this::showAuthDialog);
        });
        JButton exitBtn = makeActionButton("Exit", 14, 120, 36);
        exitBtn.addActionListener(e -> runUiAction(() -> {
            try {
                game.exitCampaign();
                appendLog("Exited campaign and saved profile.");
                // show post-signup actions after exiting
                setActionBarPostSignup();
            } catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Exit Failed", JOptionPane.WARNING_MESSAGE);
            }
        }));
        JPanel right = new JPanel(new GridLayout(0,1,4,4));
        right.add(logout);
        right.add(exitBtn);
        actionBarPanel.add(right, BorderLayout.EAST);

        actionBarPanel.revalidate();
        actionBarPanel.repaint();
    }

    /** Action bar layout while the party is inside an inn room. */
    private void setActionBarInn() {
        actionBarPanel.removeAll();

        JPanel center = new JPanel();
        center.setLayout(new java.awt.GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(8, 40, 8, 40);

        JButton shopBtn = makeActionButton("Shop", 28, 280, 80);
        shopBtn.addActionListener(e -> SwingUtilities.invokeLater(this::showShopDialog));

        JButton recruitBtn = makeActionButton("Recruit", 28, 200, 80);
        recruitBtn.addActionListener(e -> SwingUtilities.invokeLater(this::showRecruitDialog));

        JButton leaveBtn = makeActionButton("Leave Inn", 28, 280, 80);
        leaveBtn.addActionListener(e -> {
            appendLog("You leave the inn and return to the campaign.");
            setActionBarCampaign();
        });

        gbc.gridx = 0; gbc.gridy = 0;
        center.add(shopBtn, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        center.add(recruitBtn, gbc);
        gbc.gridx = 2; gbc.gridy = 0;
        center.add(leaveBtn, gbc);

        actionBarPanel.add(center, BorderLayout.CENTER);

        // Left: small status controls
        JButton showStatusBtn = makeActionButton("Show Status", 14, 140, 36);
        showStatusBtn.addActionListener(e -> runUiAction(this::showStatus));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(showStatusBtn);
        actionBarPanel.add(left, BorderLayout.WEST);

        // Right: display current gold while in inn, then stack Exit above Logout
        JPanel right = new JPanel(new GridLayout(0,1,4,4));
        innGoldLabel = new JLabel();
        updateInnGoldLabel();
        right.add(innGoldLabel);

        // Show login button when no profile is active
        if (game.getCurrentProfile() == null) {
            JButton loginBtn = makeActionButton("Log In", 12, 100, 28);
            loginBtn.addActionListener(e -> runUiAction(this::showAuthDialog));
            right.add(loginBtn);
        }

        JButton exitInnBtn = makeActionButton("Exit", 12, 100, 28);
        exitInnBtn.addActionListener(e -> runUiAction(() -> {
            try {
                game.exitCampaign();
                appendLog("Exited campaign and saved profile.");
                SwingUtilities.invokeLater(this::setActionBarPostSignup);
            } catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Exit Failed", JOptionPane.WARNING_MESSAGE);
            }
        }));
        exitInnBtn.setEnabled(game.getCurrentProfile() != null && game.getCurrentProfile().isCampaignActive());
        right.add(exitInnBtn);

        JButton logout = makeActionButton("Log Out", 14, 120, 36);
        logout.addActionListener(e -> {
            try { game.save(); } catch (Exception ignored) {}
            try { if (game != null) game.logout(); } catch (Exception ignored) {}
            appendLog("Logged out.");
            setActionBarIdle();
            SwingUtilities.invokeLater(this::showAuthDialog);
        });
        right.add(logout);
        actionBarPanel.add(right, BorderLayout.EAST);

        actionBarPanel.revalidate();
        actionBarPanel.repaint();
    }

    private void enterNextRoom() {
        ensureProfileSelected();

        CampaignEncounter encounter = game.createCampaignEncounter();
        narrateRoomIntro(encounter);

        CampaignResult result;
        if (encounter.isBattle()) {
            // switch action bar to battle mode while interactive battle runs on a background thread
            setActionBarBattle();
            Thread battleThread = new Thread(() -> {
                boolean won = runManualBattleInteractive(encounter);
                CampaignResult resolved = game.resolveCampaignEncounter(encounter, won);
                // update UI on EDT after battle resolution
                SwingUtilities.invokeLater(() -> {
                    narrateRoomOutcome(resolved);
                    refreshPartySelectors();
                    refreshRecruits();
                    setActionBarIdle();
                });
            }, "battle-thread");
            battleThread.start();
            return; // UI will be updated after background battle completes
        } else {
            result = game.resolveCampaignEncounter(encounter, false);
            appendLog("Narrator: Warm light spills across wooden tables as your party enters the inn.");
            appendLog("Narrator: The innkeeper tends to wounds and refills your flasks.");
            // Switch to Inn action bar while the player is in the inn
            SwingUtilities.invokeLater(this::setActionBarInn);
        }

        narrateRoomOutcome(result);

        refreshPartySelectors();
        refreshRecruits();

        if (game.isCampaignComplete()) {
            appendLog("Campaign complete. Final score: " + game.getCurrentProfile().getCampaignScore());
            game.save();
        }
    }

    private void narrateRoomIntro(CampaignEncounter encounter) {
        appendLog("");
        appendLog("=== Room " + encounter.getRoomNumber() + " ===");
        if (encounter.isBattle()) {
            appendLog("Narrator: You push open a cracked stone door and hear steel scraping in the dark.");
            appendLog("Narrator: Hostiles spotted -> " + formatEnemyGroup(encounter.getEnemies()));
            appendLog("Narrator: Choose each hero action carefully: Attack, Defend, Cast, or Wait.");
        } else {
            appendLog("Narrator: Lanternlight flickers ahead. You have found an inn room.");
        }
    }

    private void narrateRoomOutcome(CampaignResult result) {
        appendLog("Room " + result.getRoomNumber() + " => " + result.getRoomType());
        if (result.getRoomType() == CampaignResult.RoomType.BATTLE) {
            if (result.isBattleWon()) {
                appendLog("Narrator: Victory. The corridor falls silent as your party regroups.");
                appendLog("Rewards: +" + result.getExpGained() + " EXP, +" + result.getGoldGained() + " gold.");
            } else {
                appendLog("Narrator: Defeat. Your party retreats and is restored at the nearest inn.");
                appendLog("Penalties applied: gold and experience progress reduced.");
            }
        } else {
            appendLog("Inn room complete. Party restored and ready for the next chamber.");
        }
    }

    private boolean runManualBattle(CampaignEncounter encounter) {
        List<Hero> party = game.getCurrentProfile().getActiveParty();
        List<Hero> enemies = encounter.getEnemies();

        appendLog("Battle Start: " + party.size() + " hero(es) vs " + enemies.size() + " enemy unit(s).");
        appendLog("Tip: If mana is low, Defend restores HP and mana.");

        int round = 1;
        while (isTeamAlive(party) && isTeamAlive(enemies) && round <= MANUAL_BATTLE_ROUND_LIMIT) {
            appendLog("");
            appendLog("--- Round " + round + " ---");

            tickStatuses(party);
            tickStatuses(enemies);

            List<Hero> turnOrder = buildTurnOrder(party, enemies);
            Queue<Hero> waitQueue = new LinkedList<>();

            for (Hero actor : turnOrder) {
                if (!actor.isAlive()) {
                    continue;
                }
                if (!isTeamAlive(party) || !isTeamAlive(enemies)) {
                    break;
                }
                if (actor.isStunned()) {
                    appendLog(actor.getName() + " is stunned and misses this turn.");
                    continue;
                }

                if (party.contains(actor)) {
                    runPlayerTurn(actor, party, enemies, waitQueue);
                } else {
                    runEnemyTurn(actor, enemies, party, waitQueue);
                }
            }

            while (!waitQueue.isEmpty() && isTeamAlive(party) && isTeamAlive(enemies)) {
                Hero waiter = waitQueue.poll();
                if (waiter == null || !waiter.isAlive()) {
                    continue;
                }
                if (party.contains(waiter)) {
                    Hero target = chooseTarget(enemies, "Pick a delayed attack target for " + waiter.getName());
                    if (target != null) {
                        int dmg = waiter.attack(target);
                        appendLog(waiter.getName() + " (Wait) strikes " + target.getName() + " for " + dmg + " damage. (ATK "
                            + waiter.getCurrentAttack() + " vs DEF " + target.getCurrentDefense() + ")");
                        pauseTurn();
                    }
                } else {
                    Hero target = firstAlive(party);
                    if (target != null) {
                        int dmg = waiter.attack(target);
                        appendLog(waiter.getName() + " (Wait) strikes " + target.getName() + " for " + dmg + " damage. (ATK "
                            + waiter.getCurrentAttack() + " vs DEF " + target.getCurrentDefense() + ")");
                        pauseTurn();
                    }
                }
            }

            appendLog("Party: " + summarizeTeam(party));
            appendLog("Enemies: " + summarizeTeam(enemies));
            round++;
        }

        if (round > MANUAL_BATTLE_ROUND_LIMIT && isTeamAlive(party) && isTeamAlive(enemies)) {
            appendLog("Battle reached the round limit. The enemy presses harder as your team withdraws.");
            return false;
        }

        boolean won = isTeamAlive(party) && !isTeamAlive(enemies);
        appendLog(won ? "Battle Result: You won the encounter." : "Battle Result: Your party was defeated.");
        return won;
    }

    /**
     * Interactive manual battle loop that runs off the EDT and is driven by
     * the action bar buttons for player turns (no modal turn dialogs).
     * Returns true if the player's party won.
     */
    private boolean runManualBattleInteractive(CampaignEncounter encounter) {
        List<Hero> party = game.getCurrentProfile().getActiveParty();
        List<Hero> enemies = encounter.getEnemies();

        // take snapshots the UI code will read
        battlePartySnapshot = new ArrayList<>(party);
        battleEnemiesSnapshot = new ArrayList<>(enemies);

        appendLog("Battle Start: " + party.size() + " hero(es) vs " + enemies.size() + " enemy unit(s).");
        appendLog("Tip: Use the action bar buttons to control each hero during combat.");

        battleInProgress = true;
        int round = 1;
        while (isTeamAlive(party) && isTeamAlive(enemies) && round <= MANUAL_BATTLE_ROUND_LIMIT) {
            appendLog("");
            appendLog("--- Round " + round + " ---");

            tickStatuses(party);
            tickStatuses(enemies);

            List<Hero> turnOrder = buildTurnOrder(party, enemies);

            for (Hero actor : turnOrder) {
                if (!actor.isAlive()) continue;
                if (!isTeamAlive(party) || !isTeamAlive(enemies)) break;
                if (actor.isStunned()) {
                    appendLog(actor.getName() + " is stunned and misses this turn.");
                    continue;
                }

                if (party.contains(actor)) {
                    // Player-controlled hero: wait for action bar input
                    currentBattleActor = actor;
                    playerAwaitingAction = true;
                    appendLog("Player turn: " + actor.getName() + " (use action bar)");
                    // busy-wait until player performs an action via the action bar
                    while (playerAwaitingAction && actor.isAlive() && isTeamAlive(enemies)) {
                        try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    }
                    currentBattleActor = null;
                } else {
                    // Enemy AI
                    runEnemyTurn(actor, enemies, party, battleWaitQueue);
                }
                // update snapshots
                battlePartySnapshot = new ArrayList<>(party);
                battleEnemiesSnapshot = new ArrayList<>(enemies);
            }

            // resolve waitQueue
            while (!battleWaitQueue.isEmpty() && isTeamAlive(party) && isTeamAlive(enemies)) {
                Hero waiter = battleWaitQueue.poll();
                if (waiter == null || !waiter.isAlive()) continue;
                if (party.contains(waiter)) {
                    Hero target = firstAlive(enemies);
                    if (target != null) {
                        int dmg = waiter.attack(target);
                        appendLog(waiter.getName() + " (Wait) strikes " + target.getName() + " for " + dmg + " damage. (ATK "
                                + waiter.getCurrentAttack() + " vs DEF " + target.getCurrentDefense() + ")");
                        pauseTurn();
                    }
                } else {
                    Hero target = firstAlive(party);
                    if (target != null) {
                        int dmg = waiter.attack(target);
                        appendLog(waiter.getName() + " (Wait) strikes " + target.getName() + " for " + dmg + " damage. (ATK "
                                + waiter.getCurrentAttack() + " vs DEF " + target.getCurrentDefense() + ")");
                        pauseTurn();
                    }
                }
                battlePartySnapshot = new ArrayList<>(party);
                battleEnemiesSnapshot = new ArrayList<>(enemies);
            }

            appendLog("Party: " + summarizeTeam(party));
            appendLog("Enemies: " + summarizeTeam(enemies));
            round++;
        }

        battleInProgress = false;
        boolean won = isTeamAlive(party) && !isTeamAlive(enemies);
        appendLog(won ? "Battle Result: You won the encounter." : "Battle Result: Your party was defeated.");
        return won;
    }

    private List<Hero> buildTurnOrder(List<Hero> party, List<Hero> enemies) {
        List<Hero> order = new ArrayList<>();
        for (Hero h : party) {
            if (h.isAlive()) {
                order.add(h);
            }
        }
        for (Hero h : enemies) {
            if (h.isAlive()) {
                order.add(h);
            }
        }
        order.sort(Comparator
                .comparingInt(Hero::getLevel).reversed()
                .thenComparingInt(Hero::getCurrentAttack).reversed());
        return order;
    }

    private void runPlayerTurn(Hero hero, List<Hero> allies, List<Hero> enemies, Queue<Hero> waitQueue) {
        Object[] actions = {"Attack", "Defend", "Cast", "Wait", "Inspect"};

        while (true) {
            int choice = JOptionPane.showOptionDialog(
                    null,
                    hero.getName() + " (HP " + hero.getCurrentHealth() + "/" + hero.getCurrentMaxHealth()
                            + ", MP " + hero.getCurrentMana() + "/" + hero.getCurrentMaxMana() + ")\nChoose action:",
                    "Hero Turn",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    actions,
                    actions[0]
            );

            if (choice == 4) {
                appendLog("Inspect -> Party: " + summarizeTeam(allies));
                appendLog("Inspect -> Enemies: " + summarizeTeam(enemies));
                continue;
            }

            if (choice == 1) {
                hero.defend();
                appendLog(hero.getName() + " defends and recovers HP/mana.");
                pauseTurn();
                return;
            }

            if (choice == 2) {
                List<Ability> castable = hero.getClassAbilities().stream()
                        .filter(hero::canCast)
                        .toList();
                if (castable.isEmpty()) {
                    appendLog(hero.getName() + " has no castable abilities and defaults to attack.");
                    Hero fallback = chooseTarget(enemies, "No mana. Choose attack target for " + hero.getName());
                    if (fallback != null) {
                        int dmg = hero.attack(fallback);
                        appendLog(hero.getName() + " attacks " + fallback.getName() + " for " + dmg + " damage. (ATK "
                            + hero.getCurrentAttack() + " vs DEF " + fallback.getCurrentDefense() + ")");
                        pauseTurn();
                    }
                    return;
                }

                Ability selected = chooseAbility(hero, castable);
                if (selected == null) {
                    Hero fallback = chooseTarget(enemies, "Cast cancelled. Choose attack target for " + hero.getName());
                    if (fallback != null) {
                        int dmg = hero.attack(fallback);
                        appendLog(hero.getName() + " attacks " + fallback.getName() + " for " + dmg + " damage. (ATK "
                            + hero.getCurrentAttack() + " vs DEF " + fallback.getCurrentDefense() + ")");
                        pauseTurn();
                    }
                    return;
                }

                List<Hero> targets = isSupportAbility(selected)
                        ? aliveMembers(allies)
                        : aliveMembers(enemies);
                if (targets.isEmpty()) {
                    appendLog(hero.getName() + " could not find valid targets for " + selected.getName() + ".");
                    return;
                }
                hero.castAbility(selected, targets);
                appendLog(hero.getName() + " casts " + selected.getName() + ".");
                pauseTurn();
                return;
            }

            if (choice == 3) {
                hero.waitTurn();
                waitQueue.add(hero);
                appendLog(hero.getName() + " waits and will act at end of round.");
                pauseTurn();
                return;
            }

            Hero target = chooseTarget(enemies, "Choose attack target for " + hero.getName());
            if (target != null) {
                int dmg = hero.attack(target);
                appendLog(hero.getName() + " attacks " + target.getName() + " for " + dmg + " damage. (ATK "
                    + hero.getCurrentAttack() + " vs DEF " + target.getCurrentDefense() + ")");
                pauseTurn();
            }
            return;
        }
    }

    private void runEnemyTurn(Hero enemy, List<Hero> enemyTeam, List<Hero> party, Queue<Hero> waitQueue) {
        List<Ability> castable = enemy.getClassAbilities().stream().filter(enemy::canCast).toList();
        Hero target = firstAlive(party);

        if (target == null) {
            return;
        }

        double hpPct = (double) enemy.getCurrentHealth() / Math.max(1, enemy.getCurrentMaxHealth());
        if (hpPct < 0.25 && random.nextDouble() < 0.5) {
            enemy.defend();
            appendLog(enemy.getName() + " braces and defends.");
            pauseTurn();
            return;
        }

        if (!castable.isEmpty() && random.nextDouble() < 0.40) {
            Ability ability = castable.get(random.nextInt(castable.size()));
            List<Hero> targets = isSupportAbility(ability)
                    ? aliveMembers(enemyTeam)
                    : aliveMembers(party);
            if (!targets.isEmpty()) {
                enemy.castAbility(ability, targets);
                appendLog(enemy.getName() + " casts " + ability.getName() + ".");
                pauseTurn();
                return;
            }
        }

        if (random.nextDouble() < 0.10) {
            enemy.waitTurn();
            waitQueue.add(enemy);
            appendLog(enemy.getName() + " watches for an opening (Wait).");
            pauseTurn();
            return;
        }

        int dmg = enemy.attack(target);
        appendLog(enemy.getName() + " attacks " + target.getName() + " for " + dmg + " damage. (ATK "
            + enemy.getCurrentAttack() + " vs DEF " + target.getCurrentDefense() + ")");
        pauseTurn();
    }

    private Ability chooseAbility(Hero hero, List<Ability> abilities) {
        if (abilities.isEmpty()) {
            return null;
        }

        String[] labels = new String[abilities.size()];
        for (int i = 0; i < abilities.size(); i++) {
            Ability a = abilities.get(i);
            labels[i] = a.getName() + " (" + a.getManaCost() + " MP)";
        }

        Object selected = JOptionPane.showInputDialog(
                null,
                "Choose an ability for " + hero.getName() + ":",
                "Cast Ability",
                JOptionPane.QUESTION_MESSAGE,
                null,
                labels,
                labels[0]
        );

        if (selected == null) {
            return null;
        }

        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(selected)) {
                return abilities.get(i);
            }
        }
        return abilities.get(0);
    }

    private Hero chooseTarget(List<Hero> team, String prompt) {
        List<Hero> alive = aliveMembers(team);
        if (alive.isEmpty()) {
            return null;
        }

        String[] labels = new String[alive.size()];
        for (int i = 0; i < alive.size(); i++) {
            Hero h = alive.get(i);
            labels[i] = h.getName() + " (HP " + h.getCurrentHealth() + "/" + h.getCurrentMaxHealth() + ")";
        }

        Object selected = JOptionPane.showInputDialog(
                null,
                prompt,
                "Choose Target",
                JOptionPane.QUESTION_MESSAGE,
                null,
                labels,
                labels[0]
        );

        if (selected == null) {
            return alive.get(0);
        }

        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(selected)) {
                return alive.get(i);
            }
        }
        return alive.get(0);
    }

    private boolean isSupportAbility(Ability ability) {
        String name = ability.getName();
        return "Protect".equals(name) || "Heal".equals(name) || "Replenish".equals(name);
    }

    private void tickStatuses(List<Hero> team) {
        for (Hero hero : team) {
            if (hero.isAlive()) {
                hero.processStatusEffects();
            }
        }
    }

    private boolean isTeamAlive(List<Hero> team) {
        return team.stream().anyMatch(Hero::isAlive);
    }

    private Hero firstAlive(List<Hero> team) {
        for (Hero hero : team) {
            if (hero.isAlive()) {
                return hero;
            }
        }
        return null;
    }

    private List<Hero> aliveMembers(List<Hero> team) {
        List<Hero> alive = new ArrayList<>();
        for (Hero hero : team) {
            if (hero.isAlive()) {
                alive.add(hero);
            }
        }
        return alive;
    }

    private String formatEnemyGroup(List<Hero> enemies) {
        List<String> labels = new ArrayList<>();
        for (Hero enemy : enemies) {
            labels.add(enemy.getName() + " Lv" + enemy.getLevel());
        }
        return String.join(", ", labels);
    }

    private String summarizeTeam(List<Hero> team) {
        List<String> entries = new ArrayList<>();
        for (Hero hero : team) {
            entries.add(hero.getName() + "(" + hero.getCurrentHealth() + " HP, "
                    + hero.getCurrentMana() + " MP" + (hero.isAlive() ? "" : ", DOWN") + ")");
        }
        return String.join(" | ", entries);
    }

    private void visitInn() {
        ensureProfileSelected();
        game.visitInn();
        appendLog("Visited inn. Party fully restored.");
        refreshPartySelectors();
        refreshRecruits();
        SwingUtilities.invokeLater(this::setActionBarInn);
    }
    


    private void useSelectedInventoryItem() {
        ensureProfileSelected();

        InnItem item;
        if (sharedItemList != null && sharedItemList.getSelectedValue() != null) {
            item = sharedItemList.getSelectedValue();
        } else {
            item = (InnItem) itemCombo.getSelectedItem();
        }
        Hero target = (Hero) itemTargetCombo.getSelectedItem();

        if (item == null || target == null) {
            JOptionPane.showMessageDialog(null, "Select both an item and a target hero.", "Use Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Profile cur = game.getCurrentProfile();
        if (cur == null) {
            JOptionPane.showMessageDialog(null, "No profile selected.", "Use Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int count = cur.getInventoryCount(item);
        if (count <= 0) {
            JOptionPane.showMessageDialog(null, "You don't have any " + item.getDisplayName() + " to use.", "No Item", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Apply item to target and decrement inventory
        item.applyTo(target);
        cur.removeInventoryItem(item);
        appendLog("Used " + item.getDisplayName() + " on " + target.getName() + ".");
        refreshPartySelectors();
        showStatus();
        if (sharedItemList != null) sharedItemList.repaint();
    }

    private void refreshRecruits() {
        ensureProfileSelected();

        currentRecruitCandidates.clear();
        currentRecruitCandidates.addAll(game.getRecruitableHeroes());

        DefaultComboBoxModel<Hero> model = new DefaultComboBoxModel<>();
        for (Hero hero : currentRecruitCandidates) {
            model.addElement(hero);
        }
        recruitCombo.setModel(model);

        if (currentRecruitCandidates.isEmpty()) {
            appendLog("No recruit candidates available right now.");
        } else {
            appendLog("Recruit candidates refreshed: " + currentRecruitCandidates.size() + " available.");
        }
    }

    private void recruitSelectedHero() {
        ensureProfileSelected();

        Hero candidate = (Hero) recruitCombo.getSelectedItem();
        if (candidate == null) {
            throw new IllegalArgumentException("No recruit candidate selected.");
        }

        int cost = InnServiceImpl.recruitmentCost(candidate.getLevel());
        boolean ok = game.recruitHero(candidate);

        if (ok) {
            appendLog("Recruited " + candidate.getName() + " (Level " + candidate.getLevel() + ") for " + cost + " gold.");
            currentRecruitCandidates.remove(candidate);
            refreshRecruitsModelOnly();
            refreshPartySelectors();
        } else {
            appendLog("Recruit failed (insufficient gold or party full).");
        }
    }

    private void savePartySlot() {
        ensureProfileSelected();

        Profile profile = game.getCurrentProfile();
        boolean saved = profile.saveParty(new ArrayList<>(profile.getActiveParty()));

        if (!saved) {
            appendLog("Cannot save party: all 5 save slots are used.");
            return;
        }

        game.save();
        int slotIndex = profile.getSavedParties().size() - 1;
        appendLog("Saved current party to slot " + slotIndex + ".");
    }

    private void createOpponentAndRunPvp() {
        ensureProfileSelected();

        // If the PvP dialog is visible, close it so we continue in the main UI.
        if (pvpPanel != null) {
            java.awt.Window pvpd = SwingUtilities.getWindowAncestor(pvpPanel);
            if (pvpd != null) pvpd.dispose();
        }

        Profile player = game.getCurrentProfile();
        String playerName = player.getPlayerName();
        int winsBefore = player.getPvpWins();
        int lossesBefore = player.getPvpLosses();
        ensurePlayerHasSavedParty(player);
        String opponentName = clean(opponentNameField.getText());
        if (opponentName.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Search for an opponent. Type their username into the opponent field and click 'Start PvP'.",
                    "No Opponent Specified",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Profile existing = game.loadProfile(opponentName);
        if (existing == null) {
            JOptionPane.showMessageDialog(null,
                    "No profile named '" + opponentName + "' was found. Did you type their user correctly?",
                    "Opponent Not Found",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (existing.getSavedParties().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Opponent '" + opponentName + "' has no saved parties. Ask them to save a party before PvP.",
                    "No Saved Party",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Present opponent's saved party and ask the user to Fight or Leave.
        List<Hero> enemySavedParty = existing.getSavedParties().get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("Opponent: ").append(opponentName).append("\n");
        sb.append("Saved party (slot 0):\n");
        for (Hero h : enemySavedParty) {
            sb.append("- ").append(formatHero(h)).append("\n");
        }

        JTextArea enemyView = new JTextArea(sb.toString());
        enemyView.setEditable(false);
        enemyView.setLineWrap(true);
        enemyView.setWrapStyleWord(true);
        enemyView.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(enemyView);
        scroll.setPreferredSize(new java.awt.Dimension(420, 180));

        Object[] options = {"Fight", "Leave"};
        int choice = JOptionPane.showOptionDialog(
                null,
                scroll,
                "Challenge " + opponentName + "?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice != JOptionPane.YES_OPTION) {
            appendLog("PvP cancelled: not fighting " + opponentName + ".");
            return;
        }
        // Restore player as the active profile before starting PvP.
        game.loadProfile(playerName);

        // Run the interactive PvP session on a background thread so the EDT isn't blocked.
        setActionBarBattle();
        Thread pvpThread = new Thread(() -> {
            List<Hero> playerParty = deepCopyParty(game.getCurrentProfile().getSavedParty(0));
            List<Hero> enemyParty  = deepCopyParty(existing.getSavedParties().get(0));

            BattleResult result = runManualPvpInteractive(playerParty, enemyParty);

            // Persist/record the PvP result (controller will update league and save profiles)
            game.recordPvpResult(result, playerName, opponentName, playerParty, enemyParty);

            // Update UI on EDT after battle completes
            SwingUtilities.invokeLater(() -> {
                if (result.isDraw()) {
                    appendLog("PvP result: DRAW.");
                } else {
                    appendLog("PvP result: " + (result.getWinningTeam().stream().anyMatch(playerParty::contains) ? "YOU WON" : "YOU LOST") + ".");
                }
                showLeagueTable();
                showStatus();
                setActionBarIdle();
            });
        }, "pvp-thread");
        pvpThread.start();
        return;
    }

    /**
     * Runs an interactive manual PvP fight between two provided parties.
     * Returns the BattleResult with final teams and draw flag.
     */
    private BattleResult runManualPvpInteractive(List<Hero> party, List<Hero> enemies) {
        // Reuse the same UI-driven manual battle loop used for campaign encounters.
        battlePartySnapshot = new ArrayList<>(party);
        battleEnemiesSnapshot = new ArrayList<>(enemies);

        appendLog("PvP Start: " + party.size() + " hero(es) vs " + enemies.size() + " enemy unit(s).");
        appendLog("Tip: Use the action bar buttons to control each hero during combat.");

        battleInProgress = true;
        int round = 1;
        while (isTeamAlive(party) && isTeamAlive(enemies) && round <= MANUAL_BATTLE_ROUND_LIMIT) {
            appendLog("");
            appendLog("--- Round " + round + " ---");

            tickStatuses(party);
            tickStatuses(enemies);

            List<Hero> turnOrder = buildTurnOrder(party, enemies);

            for (Hero actor : turnOrder) {
                if (!actor.isAlive()) continue;
                if (!isTeamAlive(party) || !isTeamAlive(enemies)) break;
                if (actor.isStunned()) {
                    appendLog(actor.getName() + " is stunned and misses this turn.");
                    continue;
                }

                if (party.contains(actor)) {
                    currentBattleActor = actor;
                    playerAwaitingAction = true;
                    appendLog("Player turn: " + actor.getName() + " (use action bar)");
                    while (playerAwaitingAction && actor.isAlive() && isTeamAlive(enemies)) {
                        try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    }
                    currentBattleActor = null;
                } else {
                    runEnemyTurn(actor, enemies, party, battleWaitQueue);
                }

                battlePartySnapshot = new ArrayList<>(party);
                battleEnemiesSnapshot = new ArrayList<>(enemies);
            }

            while (!battleWaitQueue.isEmpty() && isTeamAlive(party) && isTeamAlive(enemies)) {
                Hero waiter = battleWaitQueue.poll();
                if (waiter == null || !waiter.isAlive()) continue;
                if (party.contains(waiter)) {
                    Hero target = firstAlive(enemies);
                    if (target != null) {
                        int dmg = waiter.attack(target);
                        appendLog(waiter.getName() + " (Wait) strikes " + target.getName() + " for " + dmg + " damage. (ATK "
                                + waiter.getCurrentAttack() + " vs DEF " + target.getCurrentDefense() + ")");
                        pauseTurn();
                    }
                } else {
                    Hero target = firstAlive(party);
                    if (target != null) {
                        int dmg = waiter.attack(target);
                        appendLog(waiter.getName() + " (Wait) strikes " + target.getName() + " for " + dmg + " damage. (ATK "
                                + waiter.getCurrentAttack() + " vs DEF " + target.getCurrentDefense() + ")");
                        pauseTurn();
                    }
                }
                battlePartySnapshot = new ArrayList<>(party);
                battleEnemiesSnapshot = new ArrayList<>(enemies);
            }

            appendLog("Party: " + summarizeTeam(party));
            appendLog("Enemies: " + summarizeTeam(enemies));
            round++;
        }

        battleInProgress = false;
        boolean aAlive = isTeamAlive(party);
        boolean bAlive = isTeamAlive(enemies);

        BattleResult result;
        if (!aAlive && !bAlive) {
            result = new BattleResult(new java.util.ArrayList<>(), new java.util.ArrayList<>(), true);
        } else if (aAlive) {
            result = new BattleResult(new ArrayList<>(party), new ArrayList<>(enemies), false);
        } else {
            result = new BattleResult(new ArrayList<>(enemies), new ArrayList<>(party), false);
        }

        appendLog(result.isDraw() ? "PvP ended in a draw." : (result.getWinningTeam().stream().anyMatch(party::contains) ? "You won the PvP fight." : "You lost the PvP fight."));
        return result;
    }

    /** Deep-copy a party so the UI can run a local simulation without mutating stored profiles. */
    private List<Hero> deepCopyParty(List<Hero> src) {
        List<Hero> out = new ArrayList<>();
        for (Hero h : src) {
            if (h == null) continue;
            Hero copy = new Hero(h.getName(), h.getHeroClass());
            // replicate level and basic stats by leveling up appropriately
            for (int i = 1; i < h.getLevel(); i++) copy.levelUp(copy.getHeroClass());
            out.add(copy);
        }
        return out;
    }

    private void showLeagueTable() {
        List<LeagueEntry> entries = game.getLeagueTable();
        appendLog("=== League Table ===");
        if (entries.isEmpty()) {
            appendLog("No league matches recorded yet.");
            return;
        }
        int rank = 1;
        for (LeagueEntry entry : entries) {
            appendLog(rank + ". " + entry);
            rank++;
        }
    }

    private void showHallOfFame() {
        List<Profile> profiles = game.getHallOfFame();
        appendLog("=== Hall of Fame ===");
        if (profiles.isEmpty()) {
            appendLog("No profiles recorded yet.");
            return;
        }
        for (Profile p : profiles) {
            appendLog(p.getPlayerName() + " | High Score: " + p.getHighScore()
                    + " | Gold: " + p.getGold()
                    + " | PvP W/L: " + p.getPvpWins() + "/" + p.getPvpLosses());
        }
    }

    private void showStatus() {
        ensureProfileSelected();
        Profile profile = game.getCurrentProfile();

        appendLog("=== Profile Status ===");
        appendLog("Player: " + profile.getPlayerName());
        appendLog("Gold: " + profile.getGold() + " | Campaign Room: " + profile.getCampaignRoom()
                + " | Active: " + profile.isCampaignActive()
                + " | Current Score: " + profile.getCampaignScore()
                + " | High Score: " + profile.getHighScore());
        appendLog("PvP W/L: " + profile.getPvpWins() + "/" + profile.getPvpLosses());

        List<Hero> party = profile.getActiveParty();
        if (party.isEmpty()) {
            appendLog("Active party: (empty)");
        } else {
            appendLog("Active party:");
            for (Hero hero : party) {
                appendLog("- " + formatHero(hero));
            }
        }

        // If saved parties are empty but the active party exists, auto-save
        // so the status reflects the player's current heroes (helps the demo).
        if (profile.getSavedParties().isEmpty() && !profile.getActiveParty().isEmpty()) {
            boolean autoSaved = profile.saveParty(new ArrayList<>(profile.getActiveParty()));
            if (autoSaved) {
                try { game.save(); } catch (Exception ignored) {}
                int slotIndex = profile.getSavedParties().size() - 1;
                appendLog("Auto-saved active party to slot " + slotIndex + ".");
            }
        }

        appendLog("Saved parties: " + profile.getSavedParties().size() + "/5");
        refreshPartySelectors();
    }

    private void ensurePlayerHasSavedParty(Profile player) {
        if (!player.getSavedParties().isEmpty()) {
            return;
        }
        boolean saved = player.saveParty(new ArrayList<>(player.getActiveParty()));
        if (saved) {
            game.save();
            int slotIndex = player.getSavedParties().size() - 1;
            appendLog("Auto-saved your active party to slot " + slotIndex + " for PvP.");
        }
    }

    private void refreshPartySelectors() {
        Profile profile = game.getCurrentProfile();

        DefaultComboBoxModel<Hero> heroModel = new DefaultComboBoxModel<>();
        if (profile != null) {
            for (Hero hero : profile.getActiveParty()) {
                heroModel.addElement(hero);
            }
        }

        itemTargetCombo.setModel(heroModel);
        itemTargetCombo.setRenderer(new HeroRenderer());
    }

    private void refreshRecruitsModelOnly() {
        DefaultComboBoxModel<Hero> model = new DefaultComboBoxModel<>();
        for (Hero hero : currentRecruitCandidates) {
            model.addElement(hero);
        }
        recruitCombo.setModel(model);
    }

    private void ensureProfileSelected() {
        if (game.getCurrentProfile() == null) {
            throw new IllegalStateException("No active profile. Create or load a profile first.");
        }
    }

    private static String clean(String input) {
        return input == null ? "" : input.trim();
    }

    private void appendLog(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private String formatHero(Hero hero) {
        return hero.getName()
                + " [" + hero.getHeroClass() + " Lv" + hero.getLevel() + "]"
                + " HP " + hero.getCurrentHealth() + "/" + hero.getCurrentMaxHealth()
                + " | MP " + hero.getCurrentMana() + "/" + hero.getCurrentMaxMana()
                + " | ATK " + hero.getCurrentAttack()
                + " | DEF " + hero.getCurrentDefense()
                + (hero.isAlive() ? "" : " (DOWN)");
    }

    private class HeroRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                                                                int index, boolean isSelected,
                                                                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Hero hero) {
                setText(hero.getName() + " (Lv" + hero.getLevel() + " " + hero.getHeroClass() + ")");
            }
            return this;
        }
    }

    private class RecruitRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                                                                int index, boolean isSelected,
                                                                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Hero hero) {
                int cost = InnServiceImpl.recruitmentCost(hero.getLevel());
                setText(hero.getName() + " (Lv" + hero.getLevel() + " " + hero.getHeroClass() + ", " + cost + "g)");
            }
            return this;
        }
    }

    /** Pause between turns so the user can follow actions. */
    private void pauseTurn() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
