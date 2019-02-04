package org.openRealmOfStars.player.ship;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.openRealmOfStars.gui.icons.Icons;
import org.openRealmOfStars.gui.utilies.GuiStatics;
import org.openRealmOfStars.player.PlayerInfo;
import org.openRealmOfStars.player.SpaceRace.SpaceRace;
import org.openRealmOfStars.player.SpaceRace.SpaceRaceUtility;
import org.openRealmOfStars.player.ship.shipdesign.ShipDesign;
import org.openRealmOfStars.starMap.Coordinate;
import org.openRealmOfStars.starMap.planet.Planet;
import org.openRealmOfStars.starMap.planet.construction.Construction;
import org.openRealmOfStars.utilities.DiceGenerator;
import org.openRealmOfStars.utilities.IOUtilities;

/**
 *
 * Open Realm of Stars game project
 * Copyright (C) 2016,2017,2019  Tuomo Untinen
 * Copyright (C) 2017 Lucas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/
 *
 *
 * Ship class for actual instance of a ship
 *
 */

public class Ship extends Construction {

  /**
   * Bonus for hiding espionage modules in ships.
   * So finding espionage is more difficult than
   * detecting cloacked ship. Cloaking device makes find espionage module
   * more difficult.
   */
  public static final int ESPIONAGE_HIDE = 20;
  /**
   * Ship's hull
   */
  private ShipHull hull;

  /**
   * Ship's component list
   */
  private ArrayList<ShipComponent> components;

  /**
   * Hit points per component
   */
  private int[] hullPoints;

  /**
   * Current shield level
   */
  private int shield;

  /**
   * Current armor level
   */
  private int armor;

  /**
   * Ship's image
   */
  private BufferedImage image;

  /**
   * Colonist cargo
   */
  private int colonist;
  /**
   * Metal cargo
   */
  private int metal;

  /**
   * Experience points for ship
   */
  private int experience;

  /**
   * Culture points for ship. Used for starbases and diplomatic ships
   */
  private int culture;

  /**
   * Special flags
   */
  private int specialFlags;

  /**
   * Previous trade coordinate
   */
  private Coordinate tradeCoordinates;

  /**
   * Starbase has been deployed
   */
  public static final int FLAG_STARBASE_DEPLOYED = 0x01;
  /**
   * Merchant ship has left from homeworld
   */
  public static final int FLAG_MERCHANT_LEFT_HOMEWORLD = 0x02;
  /**
   * Merchant ship has left from opponent world
   */
  public static final int FLAG_MERCHANT_LEFT_OPPONENWORLD = 0x04;

  /**
   * Bit mask for experience
   */
  private static final int BIT_MASK_EXPERIENCE = 0;

  /**
   * Bit mask for culture
   */
  private static final int BIT_MASK_CULTURE = 1;

  /**
   * Bit mask for flags
   */
  private static final int BIT_MASK_FLAGS = 2;

  /**
   * Bit mask for trade coordinate
   */
  private static final int BIT_MASK_TRADE_COORDINATE = 4;

  /**
   * Cargo type no cargo
   */
  public static final int CARGO_TYPE_NO_CARGO = 0;
  /**
   * Cargo type trade goods aka credits
   */
  public static final int CARGO_TYPE_TRADE_GOODS = 1;
  /**
   * Cargo type population
   */
  public static final int CARGO_TYPE_POPULATION = 2;
  /**
   * Cargo type metal
   */
  public static final int CARGO_TYPE_METAL = 3;
  /**
   * Cargo type troops
   */
  public static final int CARGO_TYPE_TROOPS = 4;

  /**
   * Constructor for a ship
   * @param design from where actual ship is created
   */
  public Ship(final ShipDesign design) {
    super(design.getName(), Icons.getIconByName(Icons.ICON_HULL_TECH));
    setProdCost(design.getCost());
    setMetalCost(design.getMetalCost());
    hull = design.getHull();
    components = new ArrayList<>();
    ShipComponent[] designComponents = design.getComponentList();
    hullPoints = new int[designComponents.length];
    for (int i = 0; i < designComponents.length; i++) {
      components.add(designComponents[i]);
      hullPoints[i] = design.getHull().getSlotHull();
    }
    setShield(design.getTotalShield());
    setArmor(design.getTotalArmor());
    image = ShipImage.scaleTo32x32(hull.getImage());
    setDescription(design.getDesignInfo());
    setColonist(0);
    setMetal(0);
    setExperience(0);
    setCulture(0);
    specialFlags = 0;
    tradeCoordinates = null;
  }

  /**
   * Read Ship from DataInputStream
   * @param dis DataInputStream
   * @throws IOException if there is any problem with DataInputStream
   */
  public Ship(final DataInputStream dis) throws IOException {
    super("SHIP", Icons.getIconByName(Icons.ICON_HULL_TECH));
    String tmpStr = IOUtilities.readString(dis);
    setName(tmpStr);
    setProdCost(dis.readInt());
    setMetalCost(dis.readInt());
    String hullName = IOUtilities.readString(dis);
    int raceIndex = dis.readInt();
    hull = ShipHullFactory.createByName(hullName,
        SpaceRaceUtility.getRaceByIndex(raceIndex));
    image = ShipImage.scaleTo32x32(hull.getImage());
    int count = dis.readInt();
    components = new ArrayList<>();
    hullPoints = new int[count];
    for (int i = 0; i < count; i++) {
      ShipComponent comp = ShipComponentFactory
          .createByName(IOUtilities.readString(dis));
      components.add(comp);
      hullPoints[i] = dis.readInt();
    }
    setShield(dis.readInt());
    setArmor(dis.readInt());
    setColonist(dis.readInt());
    setMetal(dis.readInt());
    byte bitMask = dis.readByte();
    if (IOUtilities.getFlag(bitMask, BIT_MASK_EXPERIENCE)) {
      setExperience(dis.readInt());
    }
    if (IOUtilities.getFlag(bitMask, BIT_MASK_CULTURE)) {
      setCulture(dis.readInt());
    }
    if (IOUtilities.getFlag(bitMask, BIT_MASK_FLAGS)) {
      specialFlags = dis.readInt();
    }
    if (IOUtilities.getFlag(bitMask, BIT_MASK_TRADE_COORDINATE)) {
      int x = dis.readInt();
      int y = dis.readInt();
      tradeCoordinates = new Coordinate(x, y);
    } else {
      tradeCoordinates = null;
    }
  }

  /**
   * Save ship to Data output Stream
   * @param dos Data Output Stream
   * @throws IOException if there is any problem with DataOutputStream
   */
  public void saveShip(final DataOutputStream dos) throws IOException {
    IOUtilities.writeString(dos, getName());
    dos.writeInt(getProdCost());
    dos.writeInt(getMetalCost());
    IOUtilities.writeString(dos, hull.getName());
    dos.writeInt(hull.getRace().getIndex());
    dos.writeInt(components.size());
    for (int i = 0; i < components.size(); i++) {
      IOUtilities.writeString(dos, components.get(i).getName());
      dos.writeInt(hullPoints[i]);
    }
    dos.writeInt(getShield());
    dos.writeInt(getArmor());
    dos.writeInt(getColonist());
    dos.writeInt(getMetal());
    byte bitMask = 0;
    if (getExperience() > 0) {
      bitMask = IOUtilities.setFlag(bitMask, BIT_MASK_EXPERIENCE, true);
    }
    if (getCulture() > 0) {
      bitMask = IOUtilities.setFlag(bitMask, BIT_MASK_CULTURE, true);
    }
    if (specialFlags != 0) {
      bitMask = IOUtilities.setFlag(bitMask, BIT_MASK_FLAGS, true);
    }
    if (tradeCoordinates != null) {
      bitMask = IOUtilities.setFlag(bitMask, BIT_MASK_TRADE_COORDINATE, true);
    }
    dos.writeByte(bitMask);
    if (IOUtilities.getFlag(bitMask, BIT_MASK_EXPERIENCE)) {
      dos.writeInt(experience);
    }
    if (IOUtilities.getFlag(bitMask, BIT_MASK_CULTURE)) {
      dos.writeInt(culture);
    }
    if (IOUtilities.getFlag(bitMask, BIT_MASK_FLAGS)) {
      dos.writeInt(specialFlags);
    }
    if (IOUtilities.getFlag(bitMask, BIT_MASK_TRADE_COORDINATE)) {
      dos.writeInt(tradeCoordinates.getX());
      dos.writeInt(tradeCoordinates.getY());
    }
  }

  @Override
  public String getDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append(getName());
    sb.append(" - ");
    sb.append(hull.getHullType().toString());
    sb.append("\n");
    sb.append("Energy: ");
    sb.append(getTotalEnergy());
    sb.append(" Init.: ");
    sb.append(getInitiative());
    sb.append("\n");
    sb.append("Cost: ");
    sb.append(getProdCost());
    sb.append(" Metal: ");
    sb.append(getMetalCost());
    sb.append("\n");
    sb.append("Speed: ");
    sb.append(getSpeed());
    sb.append(" FTL: ");
    sb.append(getFtlSpeed());
    sb.append(" Tactic: ");
    sb.append(getTacticSpeed());
    sb.append("\n");
    sb.append("Shield: ");
    sb.append(shield);
    sb.append("/");
    sb.append(getTotalShield());
    sb.append(" Armor: ");
    sb.append(armor);
    sb.append("/");
    sb.append(getTotalArmor());
    sb.append(" Hull Points: ");
    sb.append(hull.getSlotHull() * getNumberOfComponents());
    if (getTotalMilitaryPower() > 0) {
      sb.append("\n");
      sb.append("Military power: ");
      sb.append(getTotalMilitaryPower());
      if (getExperience() > 0) {
        sb.append(" Exp: ");
        sb.append(getExperience());
      }
    }
    sb.append("\n");
    sb.append("Slots: ");
    sb.append(components.size());
    sb.append("/");
    sb.append(hull.getMaxSlot());
    if (hull.getHullType() == ShipHullType.FREIGHTER) {
      sb.append("\n");
      sb.append("Cargo: ");
      sb.append(getMetal());
      sb.append(" Units:");
      sb.append(getColonist());
    }
    if (isTrooperShip()) {
      sb.append("\n");
      sb.append("Troops power ");
      sb.append(getTroopPower());
    }
    int baySize = getTotalBaySize();
    if (baySize > 0) {
      sb.append("\nFighter bays: ");
      sb.append(baySize);
    }
    return sb.toString();
  }

  /**
   * Get hull points for certain component
   * @param index Component index
   * @return Hull points
   */
  public int getHullPointForComponent(final int index) {
    if (isIndexValid(index)) {
      return hullPoints[index];
    }
    return 0;
  }

/**
 * @param index Component index
 * @return true if index is valid value
 */
private boolean isIndexValid(final int index) {
    return index >= 0 && index < hullPoints.length;
}

  /**
   * Generate shields according the shields and generator
   */
  public void regenerateShield() {
    boolean workingShields = false;
    boolean shieldsUp = false;
    boolean generatorUp = false;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);

      if (comp.getType() == ShipComponentType.SHIELD && componentIsWorking(i)
          && !shieldsUp) {
        workingShields = true;
        shieldsUp = true;
        if (shield < getTotalShield()) {
          shield++;
        }
      }
      if (comp.getType() == ShipComponentType.SHIELD_GENERATOR
          && componentIsWorking(i) && !generatorUp) {
        generatorUp = true;
        if (shield + comp.getDefenseValue() <= getTotalShield()) {
          shield = shield + comp.getDefenseValue();
        }
      }
    }
    if (!workingShields) {
      shield = 0;
    }
  }

  /**
   * Initialize value for armor and shield according the working
   * components. This should be called after combat, after fixing
   * and before combat.
   */
  public void initializeShieldAndArmor() {
    int totalArmor = 0;
    int totalShield = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (comp.getDefenseValue() > 0
          && comp.getType() == ShipComponentType.ARMOR
          && componentIsWorking(i)) {
          totalArmor = totalArmor + comp.getDefenseValue();
      }
      if (comp.getDefenseValue() > 0
          && comp.getType() == ShipComponentType.SHIELD
          && componentIsWorking(i)) {
          totalShield = totalShield + comp.getDefenseValue();
      }
    }
    setArmor(totalArmor);
    setShield(totalShield);
  }

  /**
   * Check if certain component has energy or not. Returns true if component has
   * energy. This also checks that component is functional.
   * @param index Component index
   * @return true if has energy
   */
  public boolean hasComponentEnergy(final int index) {
      return hasComponent()
              && (hasRemainingEnergy(index)
                      || !isComponentRequireEnergy(index));
  }

/**
 * @return true if has component
 */
private boolean hasComponent() {
    return components.size() > 0;
}

/**
 * @param index Component index
 * @return true if component require energy
 */
private boolean isComponentRequireEnergy(final int index) {
    return components.get(index).getEnergyRequirement() > 0;
}

/**
 * @param index Component index
 * @return true if remaining energy >= 0
 */
private boolean hasRemainingEnergy(final int index) {
    return getRemainingEnergy(index) >= 0;
}

/**
 * @param index Component index
 * @return remaining energy
 */
private int getRemainingEnergy(final int index) {
    int energy = getTotalEnergy();
    if (index < components.size()) {
        for (int i = 0; i <= index; i++) {
          ShipComponent comp = components.get(i);
          if (hullPoints[i] > 0 && comp.getEnergyRequirement() > 0) {
            energy = energy - comp.getEnergyRequirement();
          }
        }
    } else {
        energy = 0;
    }
    return energy;
}

  /**
   * Check if certain component has energy and hull points so it is functioning.
   * @param index Component index
   * @return True if component is functioning
   */
  public boolean componentIsWorking(final int index) {
    return hasComponentEnergy(index) && getHullPointForComponent(index) > 0;
  }

  /**
   * Get total energy form current component status
   * @return Total energy
   */
  public int getTotalEnergy() {
    int energy = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0 && comp.getEnergyResource() > 0) {
        energy = energy + comp.getEnergyResource();
      }
    }
    return energy;
  }

  /**
   * Get Speed depending on hull points and energy level
   * @return Speed
   */
  public int getSpeed() {
    int speed = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0 && comp.getType() == ShipComponentType.ENGINE
          && hasComponentEnergy(i) && comp.getSpeed() > speed) {
        speed = comp.getSpeed();
      }
    }
    return speed;
  }

  /**
   * Get tactic Speed depending on hull points and energy level
   * @return Speed
   */
  public int getTacticSpeed() {
    int speed = 0;
    boolean thrusters = false;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0 && comp.getType() == ShipComponentType.ENGINE
          && hasComponentEnergy(i) && comp.getTacticSpeed() > speed) {
        speed = comp.getTacticSpeed();
      }
      if (hullPoints[i] > 0 && comp.getType() == ShipComponentType.THRUSTERS
          && hasComponentEnergy(i)) {
        thrusters = true;
      }
    }
    if (thrusters && speed > 0) {
      speed = speed + 1;
    }
    return speed;
  }

  /**
   * Are there any weapons left
   * @return true if there are weapons left
   */
  public boolean hasWeapons() {
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0 && hasComponentEnergy(i) && (comp
          .getType() == ShipComponentType.WEAPON_BEAM
          || comp.getType() == ShipComponentType.WEAPON_ECM_TORPEDO
          || comp.getType() == ShipComponentType.WEAPON_HE_MISSILE
          || comp.getType() == ShipComponentType.WEAPON_PHOTON_TORPEDO || comp
              .getType() == ShipComponentType.WEAPON_RAILGUN)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Are there any bombs left
   * @return true if there are weapons left
   */
  public boolean hasBombs() {
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0 && hasComponentEnergy(i)
          && (comp.getType() == ShipComponentType.ORBITAL_BOMBS || comp
              .getType() == ShipComponentType.ORBITAL_NUKE)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get FTL Speed depending on hull points and energy level
   * @return Speed
   */
  public int getFtlSpeed() {
    int ftlSpeed = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0 && comp.getType() == ShipComponentType.ENGINE
          && hasComponentEnergy(i) && comp.getFtlSpeed() > ftlSpeed) {
          ftlSpeed = comp.getFtlSpeed();
        break;
      }
    }
    if (hull.getHullType() == ShipHullType.PROBE) {
      // Probes have faster FTL
        ftlSpeed = ftlSpeed + 1;
    }
    return ftlSpeed;
  }

  /**
   * Get ship' initiative for combat. Bigger value is better in combat
   * @return Initiative
   */
  public int getInitiative() {
    int initiative = getInitivativeByHullSize();
    initiative += increaseInitivativeByComponent();
    initiative += increaseInitivativeByEmptySpace();
    return initiative;
  }

  /**
   * Get initiative bonus from working ship components.
   * @return Increased initiative by component
   */
  private int increaseInitivativeByComponent() {
    int increased = 0;
    int speed = 0;
    int tacticSpeed = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0 && comp.getType() == ShipComponentType.ENGINE
          && hasComponentEnergy(i) && comp.getSpeed() > speed) {
        speed = comp.getSpeed();
      }
      if (hullPoints[i] > 0 && comp.getType() == ShipComponentType.ENGINE
          && hasComponentEnergy(i) && comp.getTacticSpeed() > tacticSpeed) {
        tacticSpeed = comp.getTacticSpeed();
      }
      if (hullPoints[i] > 0 && hasComponentEnergy(i)
          && comp.getInitiativeBoost() > 0) {
          increased = increased + comp.getInitiativeBoost();
      }
    }
    increased = increased + speed + tacticSpeed;
    return increased;
  }

  /**
   * Get initiative according the ship hull size.
   * @return initiative associated ship hull size
   */
  private int getInitivativeByHullSize() {
      int increased = 0;
      switch (hull.getSize()) {
      case SMALL: {
          increased = 12;
        break;
      }
      case MEDIUM: {
          increased = 8;
        break;
      }
      case LARGE: {
          increased = 4;
        break;
      }
      case HUGE: {
          increased = 0;
        break;
      }
      default:
          increased = 0;
      }
    return increased;
  }

  /**
   * Increase initiative value by empty space in ship.
   * @return Increased initiative by empty space
   */
  private int increaseInitivativeByEmptySpace() {
    int initiative;
    switch (this.hull.getMaxSlot() - components.size()) {
    case 1:
    case 0: {
        initiative = 0;
      break;
    }
    case 2:
    case 3: {
        initiative = 1;
      break;
    }
    case 4: {
        initiative = 2;
      break;
    }
    case 5: {
        initiative = 3;
      break;
    }
    case 6:
    case 7:
    case 8:
    case 9:
    case 11:
    case 10: {
        initiative = 4;
      break;
    }
    default:
        initiative = 0;
    }
    return initiative;
  }


  /**
   * Get Scanner level
   * @return scanner Lvl
   */
  public int getScannerLvl() {
    int scannerLvl = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0 && comp.getType() == ShipComponentType.SCANNER
          && hasComponentEnergy(i) && comp.getScannerRange() > scannerLvl) {
          scannerLvl = comp.getScannerRange();
      }
    }
    return scannerLvl;
  }

  /**
   * Get Scanner cloak detection level
   * @return scanner detection level
   */
  public int getScannerDetectionLvl() {
    int scannerDetectionLvl = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0
          && comp.getType() == ShipComponentType.SCANNER
          && hasComponentEnergy(i)
          && comp.getCloakDetection() > scannerDetectionLvl) {
          scannerDetectionLvl = comp.getCloakDetection();
      }
    }
    return scannerDetectionLvl;
  }

  /**
   * Get cloaking value
   * @return cloaking value
   */
  public int getCloakingValue() {
    int cloak = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0
          && comp.getType() == ShipComponentType.CLOAKING_DEVICE
          && hasComponentEnergy(i)
          && comp.getCloaking() > cloak) {
          cloak = comp.getCloaking();
      }
    }
    if (hull.getRace() == SpaceRace.TEUTHIDAES) {
      cloak = cloak + 10;
    }
    return cloak;
  }

  /**
   * Get accuracy for certain weapon
   * @param weapon ShipComponent
   * @return Accuracy
   */
  public int getHitChance(final ShipComponent weapon) {
    int accuracy = getHitChanceByWeapon(weapon);
    accuracy += increaseHitChanceByComponent();
    return accuracy;
  }

/**
 * @param weapon ShipComponent
 * @return Accuracy by weapon
 */
private int getHitChanceByWeapon(final ShipComponent weapon) {
    return weapon.getHitChance();
}

/**
 * @param Null
 * @return  accuracy
 */
private int increaseHitChanceByComponent() {
    int accuracy = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0
          && comp.getType() == ShipComponentType.TARGETING_COMPUTER
          && hasComponentEnergy(i)) {
          accuracy = accuracy + comp.getDamage();
      }
    }
    return accuracy;
}

  /**
   * Damage component by randomly
   * @param damage Amount damage to cause
   * @param shipDamage Ship Damage to more description information about damage
   * @return amount damage left aka pierced the component
   */
  public int damageComponent(final int damage, final ShipDamage shipDamage) {
    int[] componentPos = new int[components.size()];
    ShipComponent[] compList = new ShipComponent[components.size()];
    int positions = 0;
    for (int i = 0; i < hullPoints.length; i++) {
      if (hullPoints[i] > 0) {
        componentPos[positions] = i;
        compList[positions] = components.get(i);
        positions++;
      }
    }
    int target = 0;
    if (positions > 1) {
      target = DiceGenerator.getRandom(positions - 1);
    }
    int hp = hullPoints[componentPos[target]];
    hullPoints[componentPos[target]] = hullPoints[componentPos[target]]
        - damage;
    if (hullPoints[componentPos[target]] < 0) {
      // No negative hull points to components
      hullPoints[componentPos[target]] = 0;
    }
    if (compList[target] != null) {
      if (hullPoints[componentPos[target]] == 0) {
        shipDamage.addText(compList[target].getName() + " is destroyed!");
      } else {
        shipDamage.addText(compList[target].getName() + " damaged!");
      }
    }
    int result = damage - hp;
    if (hp == 0) {
      // Ship has been destroyed by attack nothing to pierce...
      result = 0;
    }
    return result;
  }

  /**
   * Fix ship either one hull point or full repair
   * @param fullFix True to fully repair the ship
   */
  public void fixShip(final boolean fullFix) {
    int maxHPperSlot = getHull().getSlotHull();
    if (fullFix) {
      for (int i = 0; i < hullPoints.length; i++) {
        hullPoints[i] = maxHPperSlot;
      }
    } else {
      for (int i = 0; i < hullPoints.length; i++) {
        if (hullPoints[i] < maxHPperSlot) {
          // Repair one point
          hullPoints[i] = hullPoints[i] + 1;
          break;
        }
      }
    }
    initializeShieldAndArmor();
  }

  /**
   * Damage information for destroyed
   */
  private static final int DAMAGE_DESTROYED = -2;

  /**
   * Get accuracy for certain weapon
   * @param weapon ShipComponent
   * @return 1 No damage, not even dent
   *         0 No damage, but shield or armor got lower
   *         -1 Got damage
   *         -2 Destroyed
   */
  public ShipDamage damageBy(final ShipComponent weapon) {
    int damage = 0;
    switch (weapon.getType()) {
    case WEAPON_BEAM:
    case WEAPON_PHOTON_TORPEDO: {
      damage = weapon.getDamage();
      damage = damage - this.getShield();
      int chance = 5 + getExperience();
      if (weapon.getType() == ShipComponentType.WEAPON_BEAM) {
        // Beam weapons have biggest chance to penetrate shields
        // and armor but they have shortest range.
        chance = 10 + getExperience();
      }
      if (damage > 0) {
        this.setShield(this.getShield() - 1);
      } else {
        if (this.getShield() / 2 <= weapon.getDamage()
            || DiceGenerator.getRandom(99) < chance) {
          this.setShield(this.getShield() - 1);
          return new ShipDamage(ShipDamage.NO_DAMAGE, "Attack hit the shield!");
        }
        return new ShipDamage(ShipDamage.NO_DAMAGE_NO_DENT,
            "Attack deflected to shield!");
      }
      damage = damage - this.getArmor() / 2;
      if (damage >= 0) {
        this.setArmor(this.getArmor() - 1);
      } else {
        damage = damage + this.getArmor() / 2;
        if (this.getArmor() / 4 <= damage
           || DiceGenerator.getRandom(99) < chance) {
          this.setArmor(this.getArmor() - 1);
          return new ShipDamage(ShipDamage.NO_DAMAGE,
              "Attack hit the armor!");
        }
        return new ShipDamage(ShipDamage.NO_DAMAGE,
            "Attack deflected to armor!");
      }
      break;
    }
    case PLASMA_BEAM: {
      damage = weapon.getDamage();
      damage = damage - this.getShield();
      if (damage > 0) {
        this.setShield(this.getShield() - 1);
      } else {
        this.setShield(this.getShield() - 1);
        return new ShipDamage(ShipDamage.NO_DAMAGE, "Attack hit the shield!");
      }
      damage = damage - this.getArmor();
      if (damage >= 0) {
        this.setArmor(this.getArmor() - 1);
      } else {
        this.setArmor(this.getArmor() - 1);
        return new ShipDamage(ShipDamage.NO_DAMAGE,
            "Attack hit the armor!");
      }
      break;
    }
    case WEAPON_RAILGUN:
    case WEAPON_HE_MISSILE: {
      damage = weapon.getDamage();
      damage = damage - this.getArmor();
      if (damage > 0) {
        this.setArmor(this.getArmor() - 1);
      } else {
        if (this.getArmor() / 2 <= weapon.getDamage()
            || DiceGenerator.getRandom(99) < 5) {
          this.setArmor(this.getArmor() - 1);
          return new ShipDamage(ShipDamage.NO_DAMAGE,
              "Attack hit the armor!");
        }
        return new ShipDamage(ShipDamage.NO_DAMAGE_NO_DENT,
            "Attack deflected to armor!");
      }
      damage = damage - this.getShield() / 2;
      if (damage >= 0) {
        this.setShield(this.getShield() - 1);
      } else {
        damage = damage + this.getShield() / 2;
        if (this.getShield() / 4 <= damage
            || DiceGenerator.getRandom(99) < 5) {
          this.setShield(this.getShield() - 1);
          return new ShipDamage(ShipDamage.NO_DAMAGE,
              "Attack hit the shield!");
        }
        return new ShipDamage(ShipDamage.NO_DAMAGE,
            "Attack deflected to shield!");
      }
      break;
    }
    case WEAPON_ECM_TORPEDO: {
      damage = weapon.getDamage();
      this.setShield(this.getShield() - damage);
      return new ShipDamage(ShipDamage.NO_DAMAGE,
          "Attacked damage shield by " + damage + "!");
    }
    default:
      /* Not a weapon */break;
    }
    ShipDamage shipDamage;
    if (damage > 0) {
      shipDamage = new ShipDamage(ShipDamage.DAMAGED,
        "Attack hit causing " + damage + " damage!");
    } else {
      shipDamage = new ShipDamage(ShipDamage.NO_DAMAGE,
          "Attack hit but caused no damage!");
    }
    while (damage > 0) {
      damage = damageComponent(damage, shipDamage);
    }
    if (getHullPoints() == 0) {
      shipDamage.setValue(DAMAGE_DESTROYED);
      shipDamage.addText(getName() + " is destroyed!");
    }
    return shipDamage;
  }

  /**
   * Get defense value for ship
   * @return defense value for ship
   */
  public int getDefenseValue() {
    int defenseValue = getDefenseValueByShipHullSize();
    defenseValue += increaseDefenseValueWithJammer();
    if (getTacticSpeed() == 0) {
        defenseValue = defenseValue - 15;
    }
    boolean cloak = false;
    boolean thrusters = false;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0
          && comp.getType() == ShipComponentType.THRUSTERS
          && hasComponentEnergy(i)) {
        thrusters = true;
      }
      if (hullPoints[i] > 0
          && comp.getType() == ShipComponentType.CLOAKING_DEVICE
          && hasComponentEnergy(i)
          && comp.getCloaking() > 0) {
        cloak = true;
      }
    }
    if (cloak) {
      defenseValue = defenseValue + 5;
    }
    if (thrusters) {
      defenseValue = defenseValue + 5;
    }
    return defenseValue;
  }

  /**
   * Get fighter bay value for ship
   * @return size of fighter bays in ship
   */
  public int getFighterBaySize() {
    int size = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0
          && comp.getType() == ShipComponentType.FIGHTER_BAY
          && hasComponentEnergy(i)) {
        size = size + comp.getBaySize();
      }
    }
    return size;
  }

  /**
   * @return defenseValue by ship hull size
   */
  private int getDefenseValueByShipHullSize() {
    int defenseValue;
    switch (hull.getSize()) {
    case SMALL:
        defenseValue = 10;
      break;
    case MEDIUM:
        defenseValue = 5;
      break;
    case LARGE:
        defenseValue = 0;
      break;
    case HUGE:
        defenseValue = -5;
      break;
    default:
        defenseValue = 0;
    }
    return defenseValue;
  }

  /**
   * @return defenseValue
   */
  private int increaseDefenseValueWithJammer() {
    int defenseValue = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0 && comp.getType() == ShipComponentType.JAMMER
          && hasComponentEnergy(i)) {
          defenseValue = defenseValue + comp.getDefenseValue();
      }
    }
    return defenseValue;
  }

  /**
   * Get number of components in ship's component list
   * @return number of components
   */
  public int getNumberOfComponents() {
    return components.size();
  }

  /**
   * Is Ship colony ship or not
   * @return True if ship has functional colony module, otherwise false
   */
  public boolean isColonyShip() {
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0 && comp.getType() == ShipComponentType.COLONY_MODULE
          && hasComponentEnergy(i)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Is ship privaatering ship or not
   * @return True if ship hull is privateer
   */
  public boolean isPrivateeringShip() {
    if (hull.getHullType() == ShipHullType.PRIVATEER) {
      return true;
    }
    return false;
  }

  /**
   * Is Ship trooper ship or not
   * @return True if ship has functional planetary invasion module,
   * otherwise false
   */
  public boolean isTrooperShip() {
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0
          && comp.getType() == ShipComponentType.PLANETARY_INVASION_MODULE
          && hasComponentEnergy(i) && getColonist() > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Is Ship spy ship or not
   * @return True if spy ship otherwise false
   */
  public boolean isSpyShip() {
    if (getEspionageBonus() > 0 && getTotalMilitaryPower() == 0
        && (hull.getName().startsWith("Corvette")
          || hull.getName().startsWith("Probe"))) {
      return true;
    }
    return false;
  }

  /**
   * Get Troop power
   * @return Get Total troop power where improvements are taken to count
   */
  public int getTroopPower() {
    int troopPower = getColonist() * hull.getRace().getTrooperPower();
    int multiply = 100;
    boolean found = false;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0
          && comp.getType() == ShipComponentType.PLANETARY_INVASION_MODULE
          && hasComponentEnergy(i) && getColonist() > 0
          && comp.getDamage() > 0) {
        multiply = multiply + comp.getDamage();
        found = true;
      }
    }
    troopPower = troopPower * multiply / 100;
    if (!found) {
        troopPower = 0;
    }
    return troopPower;
  }

  /**
   * Ship has colony module
   * @return True if ship has colony module, otherwise false
   */
  public boolean isColonyModule() {
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (comp.getType() == ShipComponentType.COLONY_MODULE) {
        return true;
      }
    }
    return false;
  }

  /**
   * Ship has trooper module
   * @return True if ship has trooper module, otherwise false
   */
  public boolean isTrooperModule() {
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (comp
          .getType() == ShipComponentType.PLANETARY_INVASION_MODULE) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get Ships damage level as color
   * @return one of these:
   * GuiStatics.COLOR_GREEN_TEXT
   * GuiStatics.COLOR_YELLOW_TEXT
   * GuiStatics.COLOR_RED_TEXT
   */
  public Color getDamageColor() {
    int damageLvl = 0;
    for (int i = 0; i < hullPoints.length; i++) {
      if (hullPoints[i] < hull.getSlotHull() && hullPoints[i] > 0
          && damageLvl < 1) {
        damageLvl = 1;
      }
      if (hullPoints[i] == 0 && damageLvl < 2) {
        damageLvl = 2;
      }
    }
    switch (damageLvl) {
    case 0:
      return GuiStatics.COLOR_GREEN_TEXT;
    case 1:
      return GuiStatics.COLOR_YELLOW_TEXT;
    case 2:
      return GuiStatics.COLOR_RED_TEXT;
    default:
      return GuiStatics.COLOR_RED_TEXT;
    }

  }

  /**
   * Get ship component by index. Can return null if index is out list.
   * @param index Component list index
   * @return ShipComponent or null
   */
  public ShipComponent getComponent(final int index) {
    if (index >= 0 && index < components.size()) {
      return components.get(index);
    }
    return null;
  }

  /**
   * Get ship's hull
   * @return Ship hull
   */
  public ShipHull getHull() {
    return hull;
  }

  /**
   * Get ship's image
   * @return Ship's image as buffered image
   */
  public BufferedImage getImage() {
    return image;
  }

  /**
   * Get maximum shield for ship. This is very useful information
   * when ship is being design and shown on ship view
   * @return Maximum shield
   */
  public int getTotalShield() {
    int totalShield = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (comp.getDefenseValue() > 0
          && comp.getType() == ShipComponentType.SHIELD) {
          totalShield = totalShield + comp.getDefenseValue();
      }
    }
    return totalShield;
  }

  /**
   * Maximum weapon range, theoritical, no weapon will have this range really
   */
  private static final int MAX_WEAPON_RANGE = 999;

  /**
   * Get Weapon range for component index
   * @param componentIndex Ship's component index
   * @return Weapon range for component
   */
  private int getWeaponRange(final int componentIndex) {
    if (componentIndex >= 0 && componentIndex < components.size()) {
      ShipComponent comp = components.get(componentIndex);
      if (comp.isWeapon()) {
        int range = comp.getWeaponRange();
        if (isStarBase()) {
          if (getFlag(FLAG_STARBASE_DEPLOYED)) {
            range++;
          } else {
            range = 0;
          }
        }
      return range;
      }
    }
    return 0;
  }

  /**
   * Get Weapon range for component
   * @param weapon Ship's weapon component
   * @return Weapon range for component
   */
  public int getWeaponRange(final ShipComponent weapon) {
    if (weapon.isWeapon()) {
      int range = weapon.getWeaponRange();
      if (isStarBase()) {
        if (getFlag(FLAG_STARBASE_DEPLOYED)) {
          range++;
        } else {
          range = 0;
        }
      }
      return range;
    }
    return 0;
  }

  /**
   * Get the smallest weapon range
   * @return range
   */
  public int getMinWeaponRange() {
    int range = MAX_WEAPON_RANGE;
    for (int i = 0; i < components.size(); i++) {
      int componentRange = getWeaponRange(i);
      if (componentRange < range) {
        range = componentRange;
      }
    }
    return range;
  }

  /**
   * Get the biggest weapon range
   * @return range
   */
  public int getMaxWeaponRange() {
    int range = 0;
    for (int i = 0; i < components.size(); i++) {
      int componentRange = getWeaponRange(i);
      if (componentRange > range) {
        range = componentRange;
      }
    }
    return range;
  }

  /**
   * Get maximum armor for ship. This is very useful information
   * when ship is being design and shown on ship view
   * @return Maximum armor
   */
  public int getTotalArmor() {
    int totalArmor = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (comp.getDefenseValue() > 0
          && comp.getType() == ShipComponentType.ARMOR) {
          totalArmor = totalArmor + comp.getDefenseValue();
      }
    }
    return totalArmor;
  }

  /**
   * Get Total amount of hull point
   * @return Maximum hull points
   */
  public int getMaxHullPoints() {
    return hull.getSlotHull() * hullPoints.length;
  }

  /**
   * Get current hull points
   * @return hull points
   */
  public int getHullPoints() {
    int hullPoint = 0;
    for (int i = 0; i < hullPoints.length; i++) {
        hullPoint = hullPoint + hullPoints[i];
    }
    return hullPoint;
  }

  /**
   * Get current shield level
   * @return Current Shield level
   */
  public int getShield() {
    return shield;
  }

  /**
   * Shield value cannot be set under zero. Minimum value is set to zero.
   * @param shield New shield value
   */
  public void setShield(final int shield) {
    if (shield >= 0) {
      this.shield = shield;
    } else {
      this.shield = 0;
    }
  }

  /**
   * Get current armor level
   * @return current armor level
   */
  public int getArmor() {
    return armor;
  }

  /**
   * Armor value cannot be set under zero. Minimum value is set to zero.
   * @param armor New armor value
   */
  public void setArmor(final int armor) {
    if (armor >= 0) {
      this.armor = armor;
    } else {
      this.armor = 0;
    }
  }

  /**
   * Get number of colonist on board
   * @return number of colonist
   */
  public int getColonist() {
    return colonist;
  }

  /**
   * Set amount of colonist on board
   * @param colonist on board
   */
  public void setColonist(final int colonist) {
    this.colonist = colonist;
  }

  /**
   * Get amount metal in cargo bay
   * @return Metal in cargo bay
   */
  public int getMetal() {
    return metal;
  }

  /**
   * Set metal in cargo bay
   * @param metal in cargo bay
   */
  public void setMetal(final int metal) {
    this.metal = metal;
  }

  /**
   * How much metal can be fit to cargo space
   * @return Cargo room for metal
   */
  public int getFreeCargoMetal() {
    int freeCargoMetal = 0;
    if (hull.getHullType() == ShipHullType.FREIGHTER) {
        freeCargoMetal = hull.getMaxSlot() - getNumberOfComponents();
        freeCargoMetal = freeCargoMetal - getColonist() / 2
                                        - getMetal() / 10
                                        - getColonist() % 2;
        freeCargoMetal = freeCargoMetal * 10;
    }
    return freeCargoMetal;
  }

  /**
   * How many colonists can be fit to cargo space
   * @return Cargo room for colonists
   */
  public int getFreeCargoColonists() {
    int freeCargoColonists = 0;
    if (hull.getHullType() == ShipHullType.FREIGHTER) {
        freeCargoColonists = hull.getMaxSlot() - getNumberOfComponents();
        freeCargoColonists = freeCargoColonists - (getColonist() / 2)
            - (getMetal() / 10);
        freeCargoColonists = freeCargoColonists * 2;
    }
    return freeCargoColonists;
  }

  /**
   * Calculate military power of design. Design needs to have at least single
   * weapon to be a military ship
   * @return Military power
   */
  public int getTotalMilitaryPower() {
    double power = 0;
    boolean militaryShip = false;
    power = getHull().getSlotHull() * getHull().getMaxSlot();
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if ((comp.getType() == ShipComponentType.WEAPON_BEAM
          || comp.getType() == ShipComponentType.WEAPON_RAILGUN
          || comp.getType() == ShipComponentType.WEAPON_HE_MISSILE
          || comp.getType() == ShipComponentType.WEAPON_PHOTON_TORPEDO)
          && componentIsWorking(i)) {
        militaryShip = true;
        power = power + comp.getDamage();
      }
      if (comp.getType() == ShipComponentType.WEAPON_ECM_TORPEDO
          && componentIsWorking(i)) {
        power = power + comp.getDamage() / 2.0;
      }
      if (comp.getType() == ShipComponentType.ARMOR
          || comp.getType() == ShipComponentType.SHIELD) {
        power = power + comp.getDefenseValue();
      }
      if (comp.getType() == ShipComponentType.ENGINE
          && getHull().getHullType() != ShipHullType.STARBASE
          && componentIsWorking(i)) {
        power = power + comp.getTacticSpeed() - 1;
      }
      if (comp.getType() == ShipComponentType.TARGETING_COMPUTER
          && componentIsWorking(i)) {
        power = power + comp.getDamage() / 10.0;
      }
      if (comp.getType() == ShipComponentType.JAMMER && componentIsWorking(i)) {
        power = power + comp.getDefenseValue() / 10.0;
      }
      if (comp.getType() == ShipComponentType.FIGHTER_BAY
          && componentIsWorking(i)) {
        power = power + comp.getBaySize();
      }
    }
    if (isStarBase() && !getFlag(FLAG_STARBASE_DEPLOYED)) {
      // Only deployed starbases should have military power
      power = 0;
    }
    if (!militaryShip) {
      power = 0;
    }
    return (int) Math.round(power);
  }

  /**
   * Get Ship's experience
   * @return Experience value
   */
  public int getExperience() {
    return experience;
  }

  /**
   * Set Ship's experience
   * @param experience to set
   */
  public void setExperience(final int experience) {
    this.experience = experience;
  }

  /**
   * Get ship's culture value.
   * @return culture value
   */
  public int getCulture() {
    return culture;
  }

  /**
   * Set culture for ship
   * @param culture value to set
   */
  public void setCulture(final int culture) {
    this.culture = culture;
  }

  /**
   * Set or disable flag
   * @param flag Flag to set or disable
   * @param value True to set and false to disable
   */
  public void setFlag(final int flag, final boolean value) {
    if (value) {
      int bitmask = flag;
      specialFlags = specialFlags | bitmask;
    } else {
      int bitmask = ~flag;
      specialFlags = specialFlags & bitmask;
    }
  }

  /**
   * Get flag
   * @param flag Flag to get set or disable
   * @return true if flag has been set and false if not
   */
  public boolean getFlag(final int flag) {
    int value = specialFlags & flag;
    if (value != 0) {
      return true;
    }
    return false;
  }

  /**
   * Is ship starbase or not
   * @return True if ship is actually starbase
   */
  public boolean isStarBase() {
    return hull.getHullType() == ShipHullType.STARBASE;
  }

  /**
   * Get total research bonus, does not check if starbase is deployed
   * @return Research bonus
   */
  public int getTotalResearchBonus() {
    int result = 0;
    for (int k = 0; k < getNumberOfComponents(); k++) {
      ShipComponent comp = getComponent(k);
      if (comp != null
          && comp.getType() == ShipComponentType.STARBASE_COMPONENT
          && componentIsWorking(k) && comp.getResearchBonus() > 0) {
        result = result + comp.getResearchBonus();
      }
    }
    return result;
  }

  /**
   * Get Ship fighter bay size
   * @return Fighter bay size
   */
  public int getTotalBaySize() {
    int baySize = 0;
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (comp.getType() == ShipComponentType.FIGHTER_BAY
          && comp.getBaySize() > 0 && componentIsWorking(i)) {
        baySize = baySize + comp.getBaySize();
      }
    }
    return baySize;
  }

  /**
   * Get total credit bonus, does not check if starbase is deployed
   * @return Credit bonus
   */
  public int getTotalCreditBonus() {
    int result = 0;
    for (int k = 0; k < getNumberOfComponents(); k++) {
      ShipComponent comp = getComponent(k);
      if (comp != null
          && comp.getType() == ShipComponentType.STARBASE_COMPONENT
          && componentIsWorking(k) && comp.getCreditBonus() > 0) {
        result = result + comp.getCreditBonus();
      }
    }
    return result;
  }

  /**
   * Get total culture bonus, does not check if starbase is deployed
   * @return Culture bonus
   */
  public int getTotalCultureBonus() {
    int result = 0;
    for (int k = 0; k < getNumberOfComponents(); k++) {
      ShipComponent comp = getComponent(k);
      if (comp != null
          && comp.getType() == ShipComponentType.STARBASE_COMPONENT
          && componentIsWorking(k) && comp.getCultureBonus() > 0) {
        result = result + comp.getCultureBonus();
      }
    }
    return result;
  }

  /**
   * Get previous trade coordinate
   * @return Trade coordinates or null
   */
  public Coordinate getTradeCoordinate() {
    return tradeCoordinates;
  }

  /**
   * Set trade coordinate for ship. Set null to clear trade coordinates.
   * @param coordinate new trade coordinate
   */
  public void setTradeDistance(final Coordinate coordinate) {
    tradeCoordinates = coordinate;
  }

  /**
   * Is Trade ship? Colony and trooper is not trade ship.
   * @return True for trade ship otherwise false.
   */
  public boolean isTradeShip() {
    if (getHull().getHullType() == ShipHullType.FREIGHTER
        && !isColonyModule() && !isTrooperModule()) {
      return true;
    }
    return false;
  }

  /**
   * Get Cargo Type
   * @return CARGO_TYPE
   */
  public int getCargoType() {
    if (getColonist() > 0 && isTrooperModule()) {
      return CARGO_TYPE_TROOPS;
    }
    if (isTradeShip() && (getFlag(FLAG_MERCHANT_LEFT_HOMEWORLD)
        || getFlag(FLAG_MERCHANT_LEFT_OPPONENWORLD))) {
      return CARGO_TYPE_TRADE_GOODS;
    }
    if (getColonist() > 0 && !isTrooperModule()) {
      return CARGO_TYPE_POPULATION;
    }
    if (getMetal() > 0) {
      return CARGO_TYPE_METAL;
    }
    return CARGO_TYPE_NO_CARGO;
  }

  /**
   * Get the espionage bonus for ship.
   * @return Espionage bonus
   */
  public int getEspionageBonus() {
    for (int i = 0; i < components.size(); i++) {
      ShipComponent comp = components.get(i);
      if (hullPoints[i] > 0
          && comp.getType() == ShipComponentType.ESPIONAGE_MODULE
          && hasComponentEnergy(i)) {
        // Since ships can have only one espionage module we can
        // Stop looking here
        return comp.getEspionageBonus();
      }
    }
    return 0;

  }

  /**
   * Calculate Trader credits for single ship. Does not
   * have any kind of sanity check. Only two coordinates
   * and calculates distance between those two.
   * @param firstCoordinate Where trading began
   * @param secondCoordinate Where trading ended
   * @return Trade credits between two coordinates
   */
  public int calculateTradeCredits(final Coordinate firstCoordinate,
      final Coordinate secondCoordinate) {
    int credit = 0;
    int distance = (int) Math.round(firstCoordinate.calculateDistance(
        secondCoordinate));
    credit = distance / 10;
    if (credit < 1 && distance > 1) {
      credit = 1;
    }
    if (credit > 5) {
      credit = 5;
    }
    int freeCargo = hull.getMaxSlot() - getNumberOfComponents();
    credit = credit * freeCargo;
    if (credit > 25) {
      credit = 25;
    }
    return credit;
  }
  /**
   * Do trade with planet if trade ship.
   * Not this does not check diplomatic relationships.
   * @param planet Planet to do trade
   * @param trader Player who is making a trade
   * @return Amount of credits gained.
   */
  public int doTrade(final Planet planet, final PlayerInfo trader) {
    int credit = 0;
    if (isTradeShip() && planet.getPlanetPlayerInfo() != null) {
      if (tradeCoordinates != null
          && !tradeCoordinates.sameAs(planet.getCoordinate())) {
        credit = calculateTradeCredits(tradeCoordinates,
            planet.getCoordinate());
        if (getFlag(FLAG_MERCHANT_LEFT_HOMEWORLD)
            && trader != planet.getPlanetPlayerInfo()) {
          setFlag(FLAG_MERCHANT_LEFT_OPPONENWORLD, true);
          setFlag(FLAG_MERCHANT_LEFT_HOMEWORLD, false);
          tradeCoordinates = planet.getCoordinate();
        } else if (getFlag(FLAG_MERCHANT_LEFT_OPPONENWORLD)
            && trader == planet.getPlanetPlayerInfo()) {
          setFlag(FLAG_MERCHANT_LEFT_OPPONENWORLD, false);
          setFlag(FLAG_MERCHANT_LEFT_HOMEWORLD, true);
          tradeCoordinates = planet.getCoordinate();
        }
      } else {
        tradeCoordinates = new Coordinate(planet.getCoordinate());
        if (planet.getPlanetPlayerInfo() != null
            && planet.getPlanetPlayerInfo() == trader) {
          setFlag(FLAG_MERCHANT_LEFT_HOMEWORLD, true);
        }
        if (planet.getPlanetPlayerInfo() != null
            && planet.getPlanetPlayerInfo() != trader) {
          setFlag(FLAG_MERCHANT_LEFT_OPPONENWORLD, true);
        }
      }
    }
    return credit;
  }
}
