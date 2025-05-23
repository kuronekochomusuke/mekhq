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

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Thrusters extends Part {
    private boolean isLeftThrusters;

    public Thrusters() {
        this(0, null);
    }

    public Thrusters(int tonnage, Campaign c) {
        this(tonnage, c, false);
    }

    public Thrusters(int tonnage, Campaign c, boolean left) {
        super(tonnage, c);
        isLeftThrusters = left;
        this.name = "Thrusters";
    }

    @Override
    public Thrusters clone() {
        Thrusters clone = new Thrusters(0, campaign, isLeftThrusters);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (null != unit && unit.getEntity() instanceof Aero) {
            int priorHits = hits;
            if (isLeftThrusters) {
                hits = ((Aero) unit.getEntity()).getLeftThrustHits();
            } else {
                hits = ((Aero) unit.getEntity()).getRightThrustHits();
            }
            if (checkForDestruction
                    && hits > priorHits
                    && (hits < 4 && !campaign.getCampaignOptions().isUseAeroSystemHits())
                    && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            } else if (hits >= 4) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        int time = 0;
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            // Test of proposed errata for repair times
            if (isSalvaging()) {
                time = 600;
            } else {
                time = 90;
            }
            if (hits == 1) {
                time *= 1;
            }
            if (hits == 2) {
                time *= 2;
            }
            if (hits == 3) {
                time *= 3;
            }
            return time;
        }
        if (isSalvaging()) {
            time = 600;
        } else {
            time = 90;
        }
        return time;
    }

    @Override
    public int getDifficulty() {
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            // Test of proposed errata for repair time and difficulty
            if (isSalvaging()) {
                return -2;
            }
            if (hits == 1) {
                return -1;
            }
            if (hits == 2) {
                return 0;
            }
            if (hits == 3) {
                return 1;
            }
        }
        if (isSalvaging()) {
            return -2;
        }
        return -1;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            if (isLeftThrusters) {
                ((Aero) unit.getEntity()).setLeftThrustHits(hits);
            } else {
                ((Aero) unit.getEntity()).setRightThrustHits(hits);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Aero) {
            if (isLeftThrusters) {
                ((Aero) unit.getEntity()).setLeftThrustHits(0);
            } else {
                ((Aero) unit.getEntity()).setRightThrustHits(0);
            }
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit && unit.getEntity() instanceof Aero) {
            if (isLeftThrusters) {
                ((Aero) unit.getEntity()).setLeftThrustHits(4);
            } else {
                ((Aero) unit.getEntity()).setRightThrustHits(4);
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
    public MissingPart getMissingPart() {
        return new MissingThrusters(getUnitTonnage(), campaign, isLeftThrusters);
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        if (null != getUnit() && null != getUnit().getEntity() &&
                (getUnit().getEntity() instanceof Aero
                        && !(getUnit().getEntity() instanceof SmallCraft
                                || getUnit().getEntity() instanceof Jumpship))) {
            return false;
        }
        return hits > 0;
    }

    @Override
    public boolean isSalvaging() {
        if (null != getUnit() && null != getUnit().getEntity() &&
                (getUnit().getEntity() instanceof Aero
                        && !(getUnit().getEntity() instanceof SmallCraft
                                || getUnit().getEntity() instanceof Jumpship))) {
            return false;
        }
        return super.isSalvaging();
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(12500);
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        boolean match = false;
        if (part instanceof Thrusters) {
            Thrusters t = (Thrusters) part;
            if (t.isLeftThrusters() == isLeftThrusters) {
                match = true;
            }
        }
        return match;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "isLeftThrusters", isLeftThrusters);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("isLeftThrusters")) {
                isLeftThrusters = Boolean.parseBoolean(wn2.getTextContent());
            }
        }
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return (skillType.equals(SkillType.S_TECH_AERO) || skillType.equals(SkillType.S_TECH_VESSEL));
    }

    public boolean isLeftThrusters() {
        return isLeftThrusters;
    }

    public void setLeftThrusters(boolean b) {
        isLeftThrusters = b;
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
        return TA_GENERIC;
    }
}
