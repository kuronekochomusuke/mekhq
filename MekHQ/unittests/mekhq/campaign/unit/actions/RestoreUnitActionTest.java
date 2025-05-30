/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.unit.actions;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Player;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.parts.MissingMekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.RestoreUnitAction.IEntityCopyFactory;
import org.junit.jupiter.api.Test;

public class RestoreUnitActionTest {
    @Test
    public void restoreUnitSwitchesOutEntityAndParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Game mockGame = mock(Game.class);
        when(mockCampaign.getGame()).thenReturn(mockGame);
        Player mockPlayer = new Player(1, "Player");
        when(mockCampaign.getPlayer()).thenReturn(mockPlayer);
        Quartermaster mockQuartermaster = mock(Quartermaster.class);
        when(mockCampaign.getQuartermaster()).thenReturn(mockQuartermaster);

        int entityId = 42;
        String shortName = "Test Mek TST-01X";
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.getId()).thenReturn(entityId);
        when(mockEntity.getShortNameRaw()).thenReturn(shortName);

        UUID id = UUID.randomUUID();
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(id);
        when(unit.getEntity()).thenReturn(mockEntity);

        Entity mockNewEntity = mock(Entity.class);
        doAnswer(inv -> {
            int newId = inv.getArgument(0);
            when(mockNewEntity.getId()).thenReturn(newId);
            return null;
        }).when(mockNewEntity).setId(anyInt());
        IEntityCopyFactory mockEntityCopyFactory = mock(IEntityCopyFactory.class);
        doReturn(mockNewEntity).when(mockEntityCopyFactory).copy(eq(mockEntity));

        RestoreUnitAction action = new RestoreUnitAction(mockEntityCopyFactory);

        action.execute(mockCampaign, unit);

        verify(mockNewEntity, times(1)).setId(eq(entityId));
        verify(mockNewEntity, times(1)).setOwner(eq(mockPlayer));
        verify(mockNewEntity, times(1)).setGame(eq(mockGame));
        verify(mockNewEntity, times(1)).setExternalIdAsString(eq(id.toString()));

        verify(mockGame, times(1)).removeEntity(eq(entityId), anyInt());
        verify(mockGame, times(1)).addEntity(eq(mockNewEntity));

        verify(unit, times(1)).setEntity(eq(mockNewEntity));
        verify(unit, times(1)).removeParts();
        verify(unit, times(1)).initializeAllTransportSpace();
        verify(unit, times(1)).initializeParts(eq(true));
        verify(unit, times(1)).runDiagnostic(eq(false));
        verify(unit, times(1)).setSalvage(eq(false));
        verify(unit, times(1)).resetPilotAndEntity();
    }

    @Test
    public void restoreUnitUsingOldStrategy() {
        IEntityCopyFactory mockEntityCopyFactory = mock(IEntityCopyFactory.class);
        Campaign mockCampaign = mock(Campaign.class);
        Quartermaster mockQuartermaster = mock(Quartermaster.class);
        when(mockCampaign.getQuartermaster()).thenReturn(mockQuartermaster);
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.getShortNameRaw()).thenReturn("Test Mek TST-01X");
        Unit unit = new Unit(mockEntity, mockCampaign);

        Part part0 = mock(Part.class);
        when(part0.getUnit()).thenReturn(unit);
        when(part0.getCampaign()).thenReturn(mockCampaign);
        EquipmentPart part1 = mock(EquipmentPart.class);
        when(part1.getUnit()).thenReturn(unit);
        when(part1.getCampaign()).thenReturn(mockCampaign);
        MissingMekLocation missing0 = mock(MissingMekLocation.class);
        when(missing0.getUnit()).thenReturn(unit);
        when(missing0.getCampaign()).thenReturn(mockCampaign);
        doAnswer(inv -> {
            // Simulate MissingPart::fix calling remove(false)
            unit.removePart(missing0);
            return null;
        }).when(missing0).fix();
        MissingEquipmentPart missing1 = mock(MissingEquipmentPart.class);
        when(missing1.getUnit()).thenReturn(unit);
        when(missing1.getCampaign()).thenReturn(mockCampaign);
        doAnswer(inv -> {
            // Simulate MissingPart::fix calling remove(false)
            unit.removePart(missing1);
            return null;
        }).when(missing1).fix();

        unit.addPart(part0);
        unit.addPart(missing0);
        unit.addPart(missing1);
        unit.addPart(part1);

        RestoreUnitAction action = new RestoreUnitAction(mockEntityCopyFactory);

        action.execute(mockCampaign, unit);

        verify(part0, atLeastOnce()).needsFixing();
        verify(missing0, times(1)).fix();
        verify(missing1, times(1)).fix();
        verify(part1, atLeastOnce()).needsFixing();
    }
}
