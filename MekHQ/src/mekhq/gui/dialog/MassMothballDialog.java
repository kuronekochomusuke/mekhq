/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.UnitTableMouseAdapter;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.utilities.ReportingUtilities;

/**
 * This class handles the display of the Mass Mothball/Reactivate dialog
 *
 * @author NickAragua
 */
public class MassMothballDialog extends JDialog implements ActionListener, ListSelectionListener {
    private static final MMLogger logger = MMLogger.create(MassMothballDialog.class);

    // region Variable Declarations
    private final Map<Integer, List<Unit>> unitsByType = new HashMap<>();
    private final Map<Integer, JList<Person>> techListsByUnitType = new HashMap<>();
    private final Map<Integer, JLabel> timeLabelsByUnitType = new HashMap<>();
    private final Campaign campaign;
    private boolean activating;

    private final JPanel contentPanel = new JPanel();
    // endregion Variable Declarations

    /**
     * Constructor
     *
     * @param frame    MekHQ frame
     * @param units    An array of unit IDs to mothball/activate
     * @param campaign Campaign with which we're working
     * @param activate true to activate, otherwise false for mothball
     */
    public MassMothballDialog(final JFrame frame, final Unit[] units, final Campaign campaign, final boolean activate) {
        super(frame, "Mass Mothball/Activate");
        setLocationRelativeTo(frame);

        sortUnitsByType(units);
        this.campaign = campaign;
        this.activating = activate;

        contentPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 3;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.ipadx = 4;
        gbc.ipady = 4;
        gbc.insets = new Insets(2, 2, 2, 2);

        gbc.weightx = 3;
        JLabel instructionLabel = new JLabel();
        instructionLabel.setBorder(new LineBorder(Color.BLUE));
        instructionLabel.setText("<html>Choose the techs to carry out " +
                                       (activating ? "activation" : "mothballing") +
                                       " operations on the displayed units. <br/>" +
                                       "A * indicates that the tech is currently maintaining units.</html>");
        contentPanel.add(instructionLabel, gbc);

        gbc.weightx = 1;
        gbc.gridy++;
        addTableHeaders(gbc);

        for (int unitType : unitsByType.keySet()) {
            gbc.gridy++;
            addUnitTypePanel(unitType, gbc);
        }

        gbc.gridy++;
        addExecuteButton(activating, gbc);

        JScrollPane scrollPane = new JScrollPaneWithSpeed();
        scrollPane.setViewportView(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setMaximumSize(new Dimension(600, 600));
        scrollPane.setPreferredSize(new Dimension(600, 600));
        getContentPane().add(scrollPane);

        this.setResizable(true);
        this.pack();
        this.validate();
        setUserPreferences();
    }

    /**
     * Adds the table headers to the content pane
     *
     * @param gbc the input gridBagConstraints to use
     */
    private void addTableHeaders(GridBagConstraints gbc) {
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        JLabel labelUnitNames = new JLabel();
        labelUnitNames.setText("Units to Process");
        contentPanel.add(labelUnitNames, gbc);

        gbc.gridx = 1;
        JLabel labelTechs = new JLabel();
        labelTechs.setText("Available Techs");
        contentPanel.add(labelTechs, gbc);

        gbc.gridx = 2;
        JLabel labelPlaceHolder = new JLabel();
        labelPlaceHolder.setText(" ");
        contentPanel.add(labelPlaceHolder, gbc);
    }

    /**
     * Adds a row of units, techs and time summary to the content pane
     *
     * @param unitType the unit's type, as an int
     * @param gbc      the input gridBagConstraints to use
     */
    private void addUnitTypePanel(int unitType, GridBagConstraints gbc) {
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        JPanel unitPanel = new JPanel();
        unitPanel.setLayout(new GridLayout(unitsByType.get(unitType).size(), 1, 1, 0));

        for (Unit unit : unitsByType.get(unitType)) {
            JLabel unitLabel = new JLabel();
            unitLabel.setText(unit.getName());
            unitPanel.add(unitLabel);
        }

        unitPanel.setBorder(new LineBorder(Color.GRAY, 1));
        contentPanel.add(unitPanel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 2.0;
        JList<Person> techList = new JList<>();
        DefaultListModel<Person> listModel = new DefaultListModel<>();

        for (Person tech : campaign.getTechs()) {
            if (tech.canTech(unitsByType.get(unitType).get(0).getEntity())) {
                listModel.addElement(tech);
            }
        }

        techList.setModel(listModel);
        techList.addListSelectionListener(this);
        techList.setBorder(new LineBorder(Color.GRAY, 1));
        techList.setCellRenderer(new TechListCellRenderer());

        JScrollPane techListPane = new JScrollPaneWithSpeed();
        techListPane.setViewportView(techList);
        techListPane.setMaximumSize(new Dimension(200, 400));
        techListPane.setMinimumSize(new Dimension(200, 150));

        contentPanel.add(techListPane, gbc);
        techListsByUnitType.put(unitType, techList);

        gbc.gridx = 2;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel labelTotalTime = new JLabel();
        labelTotalTime.setBorder(new LineBorder(Color.BLUE));
        labelTotalTime.setText(getCompletionTimeText(0));
        timeLabelsByUnitType.put(unitType, labelTotalTime);
        contentPanel.add(labelTotalTime, gbc);
    }

    /**
     * Renders the mothball/activate button on the content pane
     *
     * @param activate true to activate, otherwise false for mothball
     * @param gbc      the input gridBagConstraints to use
     */
    private void addExecuteButton(boolean activate, GridBagConstraints gbc) {
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        gbc.weighty = 0.8;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton buttonExecute = new JButton();
        activating = activate;
        buttonExecute.setText(activating ? "Activate" : "Mothball");
        buttonExecute.setActionCommand(activating ?
                                             UnitTableMouseAdapter.COMMAND_ACTIVATE :
                                             UnitTableMouseAdapter.COMMAND_MOTHBALL);
        buttonExecute.addActionListener(this);
        contentPanel.add(buttonExecute, gbc);
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MassMothballDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    /**
     * Worker function that sorts out the passed-in units by unit type and stores them in the local dictionary.
     *
     * @param units Units to sort
     */
    private void sortUnitsByType(Unit[] units) {
        for (Unit unit : units) {
            int unitType = unit.getEntity().getUnitType();

            if (!unitsByType.containsKey(unitType)) {
                unitsByType.put(unitType, new ArrayList<>());
            }

            unitsByType.get(unitType).add(unit);
        }
    }

    /**
     * Event handler for the mothball/activate button.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!e.getActionCommand().equals(UnitTableMouseAdapter.COMMAND_MOTHBALL) &&
                  !e.getActionCommand().equals(UnitTableMouseAdapter.COMMAND_ACTIVATE)) {
            return;
        }

        boolean isMothballing = e.getActionCommand().equals(UnitTableMouseAdapter.COMMAND_MOTHBALL);

        for (int unitType : unitsByType.keySet()) {
            final List<Person> selectedTechs = techListsByUnitType.get(unitType).getSelectedValuesList();
            if (selectedTechs.isEmpty()) {
                continue;
            }

            int techIndex = 0;

            // this is a "naive" approach, where we assign each of the selected techs
            // to approximately # units / # techs in mothball/reactivation tasks
            for (Unit unit : unitsByType.get(unitType)) {
                UUID id = selectedTechs.get(techIndex).getId();
                Person tech = campaign.getPerson(id);
                if (isMothballing) {
                    unit.startMothballing(tech);
                } else {
                    unit.startActivating(tech);
                }
                MekHQ.triggerEvent(new UnitChangedEvent(unit));

                if (techIndex == (selectedTechs.size() - 1)) {
                    techIndex = 0;
                } else {
                    techIndex++;
                }
            }
        }
        this.setVisible(false);
    }

    /**
     * Event handler for when an item is clicked on a tech list.
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        @SuppressWarnings("unchecked") JList<Person> techList = (JList<Person>) e.getSource();

        int unitType = -1;
        // this is probably a kludge:
        // we scan the 'tech lists by unit type' dictionary to determine the unit type since the number of tech lists
        // is limited by the number of unit types, it shouldn't be too problematic for performance
        for (int key : techListsByUnitType.keySet()) {
            if (techListsByUnitType.get(key).equals(techList)) {
                unitType = key;
                break;
            }
        }

        // time to do the work is # units * 2 if mothballing * work day in minutes;
        int workTime = unitsByType.get(unitType).size() * (activating ? 1 : 2) * Unit.TECH_WORK_DAY;
        // a unit can only be mothballed by one tech, so it's pointless to assign more techs to the task
        int numTechs = Math.min(techList.getSelectedValuesList().size(), unitsByType.get(unitType).size());

        timeLabelsByUnitType.get(unitType).setText(getCompletionTimeText(workTime / numTechs));
        pack();
    }

    /**
     * Worker function that determines the "completion time" text based on the passed-in number.
     *
     * @param completionTime How many minutes to complete the work.
     *
     * @return Displayable text.
     */
    private String getCompletionTimeText(int completionTime) {
        if (completionTime > 0) {
            return String.format("Completion Time: %d minutes", completionTime);
        } else {
            return "<html>Completion Time: <span color='" +
                         ReportingUtilities.getNegativeColor() +
                         "'>Never</span></html>";
        }
    }

    /**
     * Custom list cell renderer that displays a * next to the name of a person who's maintaining units.
     *
     * @author NickAragua
     */
    private static class TechListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            Person person = (Person) value;

            boolean maintainsUnits = !person.getTechUnits().isEmpty();
            setText((maintainsUnits ? "(*) " : "") + person.getFullTitle() + " (" + person.getMinutesLeft() + " min)");

            return this;
        }
    }
}
