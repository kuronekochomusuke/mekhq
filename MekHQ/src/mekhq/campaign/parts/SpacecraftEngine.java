/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.Mek;
import megamek.common.SimpleTechLevel;
import megamek.common.SmallCraft;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.Warship;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class SpacecraftEngine extends Part {
    static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TechBase.ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(TechRating.D)
            .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    double engineTonnage;
    boolean clan;

    public SpacecraftEngine() {
        this(0, 0, null, false);
    }

    public SpacecraftEngine(int tonnage, double etonnage, Campaign c, boolean clan) {
        super(tonnage, c);
        this.engineTonnage = etonnage;
        this.clan = clan;
        this.name = "Spacecraft Engine";
    }

    @Override
    public SpacecraftEngine clone() {
        SpacecraftEngine clone = new SpacecraftEngine(getUnitTonnage(), engineTonnage, campaign, clan);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public double getTonnage() {
        return engineTonnage;
    }

    public void calculateTonnage() {
        if (null != unit) {
            clan = unit.getEntity().isClan();
            if (unit.getEntity() instanceof SmallCraft) {
                double moveFactor = unit.getEntity().getWeight() * unit.getEntity().getOriginalWalkMP();
                if (clan) {
                    engineTonnage = Math.round(moveFactor * 0.061 * 2)/2f;
                } else {
                    engineTonnage = Math.round(moveFactor * 0.065 * 2)/2f;
                }
            } else if (unit.getEntity() instanceof Jumpship) {
                if (unit.getEntity() instanceof Warship) {
                    engineTonnage = Math.round(unit.getEntity().getWeight() * 0.06 *  unit.getEntity().getOriginalWalkMP() * 2)/2f;
                } else {
                    engineTonnage = Math.round(unit.getEntity().getWeight() * 0.012 * 2)/2f;
                }
            }
        }
    }

    @Override
    public Money getStickerPrice() {
        //Add engine cost from SO p158 for advanced aerospace
        //Drive Unit + Engine + Engine Control Unit
        if (unit != null) {
            if (unit.getEntity() instanceof Warship) {
                return Money.of((500 * unit.getEntity().getOriginalWalkMP() * (unit.getEntity().getWeight() / 100))
                        + (engineTonnage * 1000)
                        + 1000);
            } else if (unit.getEntity() instanceof Jumpship) {
                // If we're a space station or jumpship, need the station keeping thrust, which is always 0.2
                return Money.of((500 * 0.2 * (unit.getEntity().getWeight() / 100))
                            + (engineTonnage * 1000)
                            + 1000);
            }
        }
        // Small craft and dropships, TM p283
        return Money.of(engineTonnage * 1000);
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof SpacecraftEngine
                && getName().equals(part.getName())
                && getTonnage() == part.getTonnage();
    }

    @Override
    public int getTechLevel() {
        if (clan) {
            return TechConstants.T_CLAN_TW;
        } else {
            return TechConstants.T_IS_TW_NON_BOX;
        }
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "engineTonnage", engineTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clan", clan);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("engineTonnage")) {
                engineTonnage = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                clan = Boolean.parseBoolean(wn2.getTextContent().trim());
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit) {
            if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setEngineHits(0);
            }
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingSpacecraftEngine(getUnitTonnage(), engineTonnage, campaign, clan);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE);
            if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setEngineHits(((Aero) unit.getEntity()).getMaxEngineHits());
            }

            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.incrementQuantity();
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (null != unit) {
            int engineHits = 0;
            int engineCrits = 0;
            if (unit.getEntity() instanceof Aero) {
                engineHits = unit.getEntity().getEngineHits();
                engineCrits = 6;
            }
            if (engineHits >= engineCrits) {
                remove(false);
            } else {
                hits = Math.max(engineHits, 0);
            }
        }
    }

    @Override
    public int getBaseTime() {
        int time = 0;
        //Per errata, small craft now use fighter engine times but still have the
        //large craft engine part
        if (null != unit && (unit.getEntity() instanceof SmallCraft && !(unit.getEntity() instanceof Dropship))) {
            if (isSalvaging()) {
                return 360;
            }
            if (hits == 1) {
                time = 100;
            } else if (hits == 2) {
                time = 200;
            } else if (hits > 2) {
                time = 300;
            }
            return time;
        }
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            // Test of proposed errata for repair times
            time = 300;
            // Light Damage
            if (hits > 0 && hits < 3) {
                time *= hits;
            // Moderate damage
            } else if (hits > 2 && hits < 5) {
                time *= (2 * hits);
            // Heavy damage
            } else if (hits > 4) {
                time *= (4 * hits);
            }
            return time;
        }
        // Removed time for isSalvaging. Can't salvage an engine.
        // Return the base 5 hours from SO if not using the improved times option
        return 300;
    }

    @Override
    public int getDifficulty() {
        // Per errata, small craft now use fighter engine difficulty table
        if (null != unit && (unit.getEntity() instanceof SmallCraft && !(unit.getEntity() instanceof Dropship))) {
            return -1;
        }
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            // Test of proposed errata for repair times and difficulty
            // Light Damage
            if (hits > 0 && hits < 3) {
                return 1;
            // Moderate damage
            } else if (hits > 2 && hits < 5) {
                return 2;
            // Heavy damage
            } else if (hits > 4) {
                return 3;
            }
        }
        // Otherwise, use the listed +1 difficulty from SO
        return 1;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setEngineHits(hits);
            }
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (isSalvaging()) {
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                // Assuming it wasn't completely integrated into the ship it was built for, where are you going to keep this?
                return "You cannot salvage a spacecraft engine. You must scrap it instead.";
            }
        }
        return null;
    }

    @Override
    public boolean isMountedOnDestroyedLocation() {
        return false;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return (skillType.equals(SkillType.S_TECH_AERO) || skillType.equals(SkillType.S_TECH_VESSEL));
    }

    @Override
    public String getLocationName() {
        if (null != unit) {
            return unit.getEntity().getLocationName(unit.getEntity().getBodyLocation());
        }
        return null;
    }

    @Override
    public int getLocation() {
        if (null != unit) {
            return unit.getEntity().getBodyLocation();
        }
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ENGINE;
    }
}
