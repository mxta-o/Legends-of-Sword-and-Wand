CSSD2203
Winter 2026
Project Requirements


General Functionality
“Legends of Sword and Wand”
Your main task is to implement the tactical role-playing-game “Legends of Sword and Wand”. The game should be implemented as a single player game or as a “hot seat” game, where two players take turns in front of the same graphical interface. Your game needs to implement the party management system (levelling up, use of items), the core battle system, the PvE campaign and the PvP campaign (at least). You will also need to design and implement a graphical user interface with different modular views for the different use cases and a database to store game data and status.

Pre-development steps
	•	Create a database. The game requires you to store data, including stats, rankings, parties or campaign status so that you can retrieve it later. To begin with, you can use a simple MySQL database (https://www.vogella.com/tutorials/MySQLJava/article.html), but in principle your application should work with any database or even files as the backend. 
	•	Use the Java Swing or Java Spring Boot frameworks for the graphical user interface. Your application needs to show multiple pieces of information at once to the user. Therefore, you may need a complex or, to put it better, modular GUI. Consider the GUI of your IDE (multiple panels with a main window at the center). Alternatively, you can split the GUI in panels. Consider using interesting UI blocks, like tabs or collapsible panels, to improve the user experience. 
	•	You are free to use a game engine.
Even though the emphasis is not on graphics or animations, a game engine may allow you to better design the workflow or the mechanics of your game. However, you must remember that the emphasis is on design. You will need to document certain elements (like use of design patterns and architectural styles). You will need to ensure that the use of the game engine does not impose obstacles on that.




Use cases
	•	As a user I should be able to create a profile and log in.
At a minimum, the profile should consist of a username and password for authentication. The main purpose of the profile is to connect persistently stored data, such as parties, scores, rankings, campaign progress, to an existing account. Once I am authenticated and logged in, I should be directed to a GUI, where I can review my saved data, and pick a game mode (e.g., PvE or PvP).
	•	As a user, I should be able to start a new PvE campaign.
I should be able to select a class for my hero and start the game. At any point in the campaign, I should be able to check on my party and my inventory and give items to my heroes. From the same interface, I should be able to explicitly ask to visit the next room. If the room is a battle room, I need to see the battle view (see user story 3). At the end of the battle, I should be informed about its results, including gained or lost experience and gold, and take actions like level up my heroes if applicable. If the room is an inn room, I need to see the inn view (see user story 4). After visiting 30 rooms, the game needs to calculate my score and store it in the database. I should be able to see my score and my ranking in my profile view (see user story 1). At this point, I can also decide if I want to keep my party or not. If I already have 5 parties stored, the game should prompt me to replace one with the new one and the replaced one should be permanently removed from the database and my profile.
	•	As a user, I should be able to go through a single battle.
I should be able to see my enemies and relevant information about them (e.g., level, attack, defense, HP), as well as my own team. From this view, I need to be able to understand the order in which each unit will act, as well as their status (ready to attack, defend or wait). I should be provided with a list of available actions for the unit whose turn it is to act. After every action, all affected hero stats (HP, mana and others) must be updated accordingly. If a party can no longer continue the battle, the result is returned to the previous view (PvE or PvP) along with the appropriate information (user’s party, enemy party, surviving heroes).
	•	As a user, I should be able to interact with the inn.
I should be informed on my party’s status (who was revived, who was healed, by how much etc.). I should be able to see the available items and purchase them if I have gold. I should be able to see all the available heroes and recruit them if I can (if I have enough gold and enough space in my party). Once I am done, I should choose to visit the next room.
	•	As a user I should be able to exit the PvE campaign.
All my progress, including party and hero status and level, my inventory and my current room should be saved in the database. I cannot exit if I am currently in a battle. I can only exit if I am at an inn or between rooms.
	•	As a user, I should be able to continue an incomplete PvE campaign.
All my progress should be loaded from the database and I should be able to see the campaign or inn view, depending on what room I left last time.
	•	As a user, I should be able to send an invitation for a PvP battle.
I need to know the exact username I want to invite. The username must have a registered profile. Both I and the invited player must have at least one saved party. When the invitation is accepted, both players are prompted to choose one of their saved parties and there are transferred to the battle view (see user story 3). When the result is determined, the database is updated with the new player stats and the updated league standings.


Term Project
Winter 2026
	•	Project Description
Legends of Sword and Wand
“Alright… Pen? Check! Paper? Check! Chocolate Milk? Check!! Now, where were we? Oh yes! Young Robert Scullion is walking in the forest. It’s not so scary. The sun shines through green leaves, the trees stand tall, the grass feels cool under his feet and smells really nice! It’s a nice spring morning and…” “Oh come on, Jim! Will you take 2 hours setting up the scene? Who do you think you are? Tolkien?!” “Chill Winston!”, said Amy, “D&D is all about the story. You need to get in the mood!” “Wait!”, said Ian, “are we supposed to be Robert Scullion? All of us?!” “Ugh….”, grunted Jim”, “do I need to explain the rules every single time one of you joins the game?!” “I thought you liked the sound of your own voice…”, smirked Winston. “Fiiiiine… But that’s the last time!”, said Jim, faking reluctance…
“Legends of Sword and Wand” is a tabletop “Dungeons & Dragons” like game. Your goal is to form a party of heroes, level them up and fight against the perils of the world or against other player parties. 
There are four hero classes:
	•	Order: The powers of the universe need to be balanced! Balance brings stability, and stability brings prosperity. Within balance you can channel your inner energy and protect those around you. Servants of order have higher intelligence, can heal and protect. Stats per level: +5 mana, +2 defense Spells or special moves:  Protect: cast a shield on all party members for 10% of each hero’s health. Costs 25 mana. Heal: heal the hero with the lowest current health for 25% of their original health points. Costs 35 mana.
	•	Chaos: The universe expands and cracks. Through the ripples untamed energy flows! Servants of chaos can harness this energy and launch devastating attacks! Stats per level: +3 attack, +5 health Spells or special moves: Fireball: Launch a fire attack that affects at most three enemy units. Cost 30 mana. Chain lightning: Target all enemy units in order. The first unit receives 100% of the attack damage and every subsequent receives 25% of the previous damage. The order in which enemy units are hit is random starting with the unit the player chooses. Cost 40 mana.
	•	Warrior: Warriors are the blunt instruments of the world. They can enforce order or cause chaos. Some are mindless brutes, others are intelligent strategist. Who would you choose? Stats per level: +2 attack, +3 defense
Spells or special moves: Berserker attack: when attacking a unit, damage 2 more units for 25% of the original damage. Cost 60 mana.
	•	Mage: Those who wield wands should be feared! Who knows what goes in their mind or whom they serve? Will they aid you or hinder you? Powerful spells that can heal or damage, all at the flick of the wand! Stats per level: +5 mana, +1 attack Spells or special moves: Replenish: replenish 30 mana points to all friendly units and 60 to self. Cost 80 mana.
You start with a hero at level 1, for whom you choose one of the four classes above. As your hero levels up, you can choose a different class to level up or the same. When you level up one class to level 5 you will gain that class’s bonus effect and your growth will be doubled. When you level up a second class to level 5, your class will become a hybrid of the two, granting your hero unique stats or abilities, according to the table below. Note: the hybrid class is now permanent and cannot change no matter how you continue to level up each class. In hybrid classes, you lose the double growth from the specialization class, but you combine the growth of the two classes that are combined.









Hybrid class name


Order
Chaos
Warrior
Mage
Hybrid class effect or special ability
Order
Priest Heal now applies to all friendly units
Heretic 
Paladin 
Prophet 

Chaos
You can now cast Fire Shield instead of Protect. Same effect as Protect, but if a protected unit is attacked it returns 10% of the damage back to the attacker.
Invoker Chain Lightning now does 50% damage for every subsequent target hit
Rogue 
Sorcerer 

Warrior
Berserker attack now heals the Paladin for 10% of their original health points before launching the attack
You can now Sneak Attack. Every time you attack, there is a 50% chance to perform an additional attack to any enemy unit for 50% of your total damage.
Knight Berserker attack now has 50% stunning the units hit (miss next turn)
Warlock 

Mage
Friendly spells (Protect, Heal, Replenish) now double their effect.
Fireball now causes double damage to all affected units.
Mana burn. Every time you attack a unit, you burn 10% of its total mana points.
Wizard Replenish now costs only 40 mana.

How do hybrid classes work? With an example…
You start with Warrior and you reach level 5. Knight becomes your specialization class and your Berserker attack has a chance to stun. Also, every time you level up you gain +4 attack and +6 defense. Then you level up your Mage class to level 5. In this case, you abandon your Knight specialization and you become a Warlock. You no longer stun your enemies, but now you can burn their mana. As a Warlock, every time you gain a level, you also gain +3 attack, +3 defense and +5 mana.
Remember! Since you lose the benefits of your specialization class when you hybridize, it may not be beneficial to always hybridize… Think carefully!
Basic Levelling up
When you first recruit a hero, you start at level 1 and you select a class. Every hero starts with 5 attack, 5 defense, 100 health points and 50 mana points. Every level you gain, your hero gains +1 attack, +1 defense, +5 health points and +2 mana points before class bonuses are applied. Every hero can level up to level 20.
Battle System: Attack, Defense and Cast
Parties consist of 1 to 5 members. Every unit acts in order, with the highest level or highest attack unit going first. Once the first unit to act is determined, the teams alternate. Every turn a unit may choose to a) attack, b) defend, c) wait, or d) cast special ability. 
When a unit attacks, it does damage equal to: UA(attack)-UD(defense) (where UA(attack) is the attack of the unit that attacks and UD(defense) is the defense of the unit that receives the attack). The total damage is removed from the defending unit’s health points plus any shielding if present. 
When a unit defends, it forfeits its turn without any action. If a unit defends they regain +10 HP and +5 mana.
When a unit waits, its action is postponed to the end of the turn. If multiple units wait, they act in a FIFO order (first to wait is the first to act when all other units have acted).
When a unit casts a special ability, their damage (if any) is calculated in the same way as in the basic attack. All other effects are applied after the end of this unit’s action. If the ability spends mana, this is deducted at the beginning of the action (to ensure that mana is enough). If mana is not enough, the unit must select another action.
If the unit is stunned as a result of a special ability, it is not able to act for one turn.
The battle ends if all units of one team run out of health points.
After the battle ends, units with no health points must be resurrected before they can be used in the next battle. Between battles, you can heal your units using food items or replenish their mana using drink items. After the battle ends, units gain experience (see Experience Gain) and you gain gold, which you can use to purchase food or drink (see Inns).
Experience and Gold Gain
Every time you win a battle, you can X amount of experience times Y enemy units. The amount of experience you gain per enemy scales according to the enemy’s level. For example, level 1 enemies give you 50 exp per unit. If you fight 3 level 1 enemies, you’ll gain 150 exp in total. Exp per enemy level is awarded according to the simple function Exp(L)=50*L, where L is the enemy’s level. After the battle ends, the experience is divided among the heroes still standing (i.e., those with HP>1). Similarly, gold per enemy is calculated as G(L) = 75*L. 
The amount of experience needed by your hero to level up is given by the following function: Exp(L) = Exp(L-1)+500+75*L+20*L2 When battling other player parties, no experience or gold is gained.
When you lose a battle, you lose 10% of your gold and 30% of your heroes gained experience in the current level (note: your heroes cannot lose levels) and you return to the last encountered inn.
Player vs Environment (PvE) In this game mode, the player follows a campaign. In its simplest form the campaign is simply walking through a dungeon. At every room, the player has a chance to encounter an enemy party or an inn. The more powerful the player is the higher the chances to encounter an enemy party. Starting at 60% for battle and 40% for inn, the balance is shifted by 3% for every 10 cumulative hero levels. For example, if you have a party with a cumulative level between 10-20, then the balance of probability is 63% for battle and 37% for inn. For a cumulative level between 90-100 the balance becomes 90%-10%. 
After 30 rooms, the campaign ends and the game calculates the player’s final score. For every hero level, the player gets 100 points. For every piece of gold, the player gets 10 points. For every bought item (see Inn) the player gets half the price for buying the item times 10. After the campaign ends, the player may choose to save their party to be used for PvP battles (see PvP). A player may save up to 5 teams.
At the end, the game keeps a hall of fame with the highest scores among the players.
Inn When visiting an inn all the player’s heroes are revived, fully healed and their mana replenished.
They can buy food and drink items accordingly:
Item
Cost
Effect
Bread
200g
+20 HP
Cheese
500g
+50 HP
Steak
1000g
+200 HP
Water
150g
+10 mana
Juice
400g
+30 mana
Wine
750g
+100 mana
Elixir
2000g
Revive+Full HP+Full mana

Unemployed heroes frequent inns in the first 10 rooms. Heroes have a random class and they can be a random level between 1-4. Level 1 heroes will join your party for free. After that they will join you for 200g per level. If your party has already 5 heroes, no heroes will be available for recruiting. 
Enemies
If a room includes a hostile encounter, the party you will battle is randomly drawn to have between 1-5 units and be between 1-10 levels. The cumulative level of the enemy party scales with your level. For example, if your party has a cumulative level of 5 you are more probable to encounter a party with a cumulative level between 0-5, while if you have a cumulative level of 40, you can encounter an enemy party between 30-40 levels. The enemies’ attack, defense and HP are also scaled accordingly, so that the battle is not too easy or too hard. Enemy units do not have any special abilities or spells. They can only attack, defend or wait (with the same effects as your heroes).
Player vs Player (PvP)
In this game mode, players can go up against other players with parties they have built during the PvE campaign. A PvP battle goes in the exact same way as a PvE battle. Winner is the player that has at least one hero standing. At the end, the game keeps a record of every player’s stats (wins and losses) in a league type of competition. Important note: depending on how the PvE campaign goes, the parties selected for PvP may not have the same strength (cumulative level or even the same number of heroes).

CSSD2203/DIGT3141 Group Project 
Winter 2026

Deliverable 1

Purpose of the First Deliverable
The purpose of this deliverable is to produce 
	•	A collection of sequence diagrams for each use of the 7 use cases presented in the Project Description Document 
	•	A list of functionalities that need be implemented (e.g. battle system, PvE, rankings etc.)
	•	An initial decomposition of your system in the form of modules and their interactions. You may use a blocks and arrows type of diagram, or if you would like to try, use a component diagram  
	•	An implementation plan in the form of a GANTT Diagram (https://en.wikipedia.org/wiki/Gantt_chart) 
	•	Test cases which will guide your development (i.e. the test cases which your code has to pass for implementing the use cases – see Test-Driven Development approach). We will expect to see initially at least 10 test cases. For example, for one turn in battle, some test cases can be
	•	Input: Current status of both parties (HP, Mana, levels)
	•	Output: Party status after one basic attack is executed. Calculated attributes (HP, Mana)
	•	Input: Current status of both parties (HP, Mana, levels)
	•	Output: Party status after one Berserker attack is executed. Calculated attributes (HP, Mana, status=stunned)
	•	The implementation of an initial set of system functionality. Some suggestions for source code implementation for deliverable 1 include:
	•	The profile creation
	•	The basic battle system 
	•	The PvE mode
	•	Correct use of the database to store party status and campaign progress.
	•	A report as discussed below in Section “What to hand In”

In this Assignment, you will learn to:
	•	Analyze user requirements and use cases
	•	Produce sequence diagrams as a requirements specification aid 
	•	Decompose a complex problem into sub-problems 
	•	Start understanding and addressing software architecture design 
	•	Produce an initial Software Design Document (SDD) from use cases
	•	Initiate implementation using design patterns. Keep in mind that the final implementation has to have six design patterns.
Assigned
Monday, January 19, 2026 (please check e-class regularly for any updates or revisions).
Due
This assignment is due Friday, February 20, 2026 by 11:59 pm (midnight-ish) by e-class submission. Please check e-class regularly for any updates regarding the submission process). One submission per group.
Late Penalty
Late project will be accepted for up to two days after the due date, with weekends counting as a single day. Submissions late up to 24 hours will incur a penalty of 10%, and submissions up to 48 hours will incur a penalty of 20%. No submissions after 48 hours of the deadline are accepted. The same time windows hold if there is an extension given by the instructor.
Group Effort
This stage of the project is expected to be a group effort, with each member of the group contributing equally in a reasonable fashion.  If it is determined that you are guilty of cheating on the assignment, you could receive a grade of zero with a notice of this offence submitted to the Dean of your home faculty for inclusion in your academic record.
What to Hand in
Your submission, as noted above, will be electronically via e-class in the form of a document (e.g. DOC or PDF) file. Please check e-class regularly for any changes related to the submission process. You are to complete the deliverable by: 
	•	providing the required documentation using the template provided to you in the Project Resources section and 
	•	the implementation of:
	•	The profile creation
	•	The basic battle system 
	•	The PvE mode
	•	Correct use of the database to store party status and campaign progress.
	•	The implementation is to be provided as a link to a GitHub repository clearly added in the Document.
	•	The project contribution attestation signed by all members OR a letter by each member outlining their own and their team’s contribution, if not all members agree on equal contribution.
Make sure each of your modules has an associated main class to execute each of such module separately, even though modules may not yet work together. For example, the visualizations can be invoked and appear in their own respective windows, or the data can be fetched and printed in the console in a tabular format.
You are encouraged to use a collaboration platform and source code repository like GitHub. If you do so, you can add the URL for the repository at the top of your submitted document (and make sure that the repository can be accessed by the TAs and the instructor). In this case, you do not have to submit your code base as an archive, but only your document as a pdf.
Guidelines for Artificial Intelligence (AI)-Generated Text
[as stated by IEEE, https://journals.ieeeauthorcenter.ieee.org/become-an-ieee-journal-author/publishing-ethics/guidelines-and-policies/submission-and-peer-review-policies/ ]
“The use of content generated by artificial intelligence (AI) in an article (including but not limited to text, figures, images, and code) shall be disclosed in the acknowledgments section of any article submitted to an IEEE publication. The AI system used shall be identified, and specific sections of the article that use AI-generated content shall be identified and accompanied by a brief explanation regarding the level at which the AI system was used to generate the content.
The use of AI systems for editing and grammar enhancement is common practice and, as such, is generally outside the intent of the above policy. In this case, disclosure as noted above is not required, but recommended.”  For your project:
	•	Please add a “Use of AI” section at the end of your Software Design Document (SDD).
	•	In that section, provide an overview of where you used Generative AI, including text, diagrams, code. Provide a list with the section and artifact (text, diagram) that AI was used.
	•	In your code, add the comment “With the use of AI” in every class where AI was applied.
	•	In your SDD, cite the model(s) and version you used for AI-generated content.
Assignment Task
In this assignment, you will craft an initial Software Design Document (SDD) that captures key ideas of the software design of your system that meets the requirements of the use cases presented in the Project Description Document. You can use the SDD Template document provided to you.
Your SDD documentation based on the given template must consist of the following sections and content: 
1.  Main Page
The main page will contain:
	•	A title for the project, and a sub-title indicating that this is the requirements documentation for that project.
	•	The Document Change Control revision history of your document in tabular form.  It should be updated as people add to and edit your requirements documentation.  It should be formatted as follows:
 Version
 Date
 Author(s)
 Summary of Changes
 
 
 
 
 
 
 
 
	•	The table of Contents.
 2.  Introduction
The Introduction section, should have the following content.
Purpose: This sub-section outlines the general objectives of the project.
Overview :This sub-section provides an overview of the problem being solved by this software and the requirements of the system.  This should be an executive summary.
References: A list of references to other documents that might provide context or otherwise assist in the understanding of this document.
The Introduction section will be similar to the one in your SRS submitted in Assignment 2.

3. Major Design Decisions 
Text describing significant design choices, and modularization criteria. Modularization criteria include high cohesion and low coupling. Explain how your design aims to achieve these criteria. 

4.  Sequence Diagrams
One sequence diagram for each of the 8 use cases described in the Project Description document. Make sure that in each diagram you use correct and complete notation capturing the corresponding use case. Each sequence diagram has to be accompanied by a short description explaining it (one paragraph).

5.  Architecture
Provide an initial decomposition of you system in terms of modules and interactions. At this stage you can use a block and arrow diagram or a UML component diagram. 
Provide a description of each module and its interaction with other modules in the form of the following tables. 
Modules
Module Name
Description 
Exposed Interface Names
Interface Description
M1
“a description”
M1:I1
M1:I2
M1:I1 “a description”
M1:I2 “another description”
M2
“another description”
M2:I3
M2:I3 “a third description” 

Interfaces
Interface Name
Operations 
Operation Descriptions
M1:I1
<return type> I1:Op1() used by M2, M4
<return type> I1:Op2(int x) used by M3 and M9
M1:I1:Op1(): “a description”
M1:I1:Op2(int x): “xxxxx”
M1:I2
<return type> M1:I2:Op3() used by M9
M1:I2:Op3() used by M1

Here we have two modules M1, and M2. M1 exposes two interfaces I1, and I2. Interface I1 has two operations Op1(), and Op2(int x), while I2 has one operation Op3(). Please note that the return type is included in the Operations column. This section will be revised in deliverables 2 & 3. 

6.  Class Diagrams and Initial Implementation 
For this section, you are to create a UML class diagram illustrating the initial design for the modules you implement for deliverable 1. It is expected that these diagrams in your report will be revised, refactored, updated, and enhanced in deliverables 2 & 3. You can use ObjectAid, PlantUML or another class extraction tool to present your class diagrams at the end. Your class diagrams should have:
	•	Classes
	•	Attributes, along with their types and visibility
	•	Methods, along with their parameters, parameter types, return types, and visibility
	•	Associations, hierarchy and implementation links
You are also to provide a textual description of each of you classes and their methods in the form of a table such as: 
Class name
Attribute/Method name
Description 
C1
attribute-name: attribute-type
“a description”
C1
<return type> method1(param-type, param-name))
“a description”

This section will be revised in deliverables 2 & 3.


Your implementation for deliverable 1 will have:
	•	The profile creation
	•	The basic battle system 
	•	The PvE mode
	•	Correct use of the database to store party status and campaign progress.
Most likely your code will be refactored in deliverables 2 & 3 to include proper design patterns and conform to better design decisions.  Make sure you document your code (JavaDoc would be highly appreciated with a potential bonus!). Make sure your code is well thought of (i.e. uses interfaces, abstract classes etc. as needed and as per the Project Description Document).  

7.  Design Patterns
For this section, discuss the design patterns you used / planning to use. You are expected to correctly implement by Deliverable 3 at least six design patterns in order to obtain full marks. The intention to implement the six design patterns (including which and how) will have to be documented in the SDD for Deliverable 1. This section of your report will be revised in deliverables 2 & 3. This section of your report would be substantially enhanced in deliverables 2 & 3. 

8.  Activities Plan, Product Backlog, and Sprint Backlog
In this section provide the following: 
	•	a complete product backlog list (i.e. what needs to be implemented as a whole). Identify the list if items to be implemented for each deliverable out of the backlog list. You are expected every four weeks to implement, refactor, integrate, and run your code.  
	•	A GANTT diagram with a schedule of your planned activities
This section of your report may be slightly revised in deliverable 2. 
Group Meeting Logs
In this Section you write minutes of each meeting, list the meeting attendance, what the topics of discussion in the meeting were, any decisions that were made, and which team members were assigned which tasks. These minutes must be submitted with the project report in each deliverable and will provide input to be used for the overall assessment of the project. 
9. Test Driven Development (TDD)
At this point you do not have fully implemented your code yet, but hopefully you have some use cases implemented. 
In this section, you will provide an initial set of test cases you consider fundamental for verifying your requirements if you had assumed you have the code implemented. Following then the concept of TDD, you write your code so that these tests will pass. Of course, testing assumes that more tests should be written once the code is implemented, but this is a starting point we can use for our course here. You should provide the final and complete list of test cases with your Deliverable 3 (complete implementation). 




For this deliverable provide a list of 10-12 test cases in the form of a table as follows:
Test ID
The unique Id of the test case
Category
Which part of the system is tested (e.g. evaluation of parameters to fetch data from the database)
Requirements Coverage
The unique ID of the requirement tested (e.g. UC1-Successful-Data-Load)
Initial Condition 
Initial conditions required for the test case to run (e.g. the system has been initiated and runs)
Procedure
The list of steps required for this test case (e.g.
1. The user provides a meal
2. The user provides a date
3. The user provides a list of ingredients and quantities
4. The user clicks on the log meal button
Expected Outcome
The expected outcome of the test case (e.g. The nutrient data is fetched from the database and appears in a table)
Notes
Any other notes you may want to add for this test case, which are also reflected in the requirements specification (e.g. the user should provide valid input)
This part corresponds to section 8 of the template provided. 
 From the point you complete and submit this deliverable you will keep on working on your implementation and complete as required the list of any additional test cases you want to include. That is this section of your report will be revised in deliverables 2 & 3.

