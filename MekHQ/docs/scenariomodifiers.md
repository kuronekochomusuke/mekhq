# Scenario Modifiers

- `modifierName` - name of the modifier, intended to be displayed in the various modifier dropdowns. Currently not
  working.
- `additionalBriefingText` - adds extra arbitrary text to the scenario briefing. Works fine.
- `benefitsPlayer` - used to indicate whether this modifier is helpful to the player or not. Unused.
- `blockFurtherEvents` - used to indicate that no further events should be processed after this modifier. Not sure if
  working or not, probably not.
- `eventTiming` - PreForceGeneration or PostForceGeneration. Indicates when the modifier is eligible to be applied.
- `forceDefinition` - a ScenarioForceTemplate definition.
- `skillAdjustment` - adjusts the skill rating of all the units generated up until this point up or down the
  indicated # of levels.
- `qualityAdjustment` - adjusts the equipment quality of all units generated from this point on up or down the
  indicated # of levels.
- `eventRecipient` - to which forces (Player, Allied, Opposing, Third, PlanetOwner) this modifier will apply.
- `battleDamageIntensity` - how many points of damage maximum to apply to the armor of every enemy unit (each unit
  gets 1-# damage, no internal damage)
- `ammoExpenditureIntensity` - how many shots of a random amount of ammo to use from every enemy unit. Probably not
  implemented.
- `unitRemovalCount` - get rid of # randomly selected units from the eventRecipient.
- `allowedMapLocations` - which map types this modifier should be applicable to. Not implemented.
- `useAmbushLogic` - pretty sure this hides some random number of units for the eventRecipient. Not sure if
  implemented.
- `switchSides` - causes units from the `eventRecepient` to switch to the opposing side if allied. Not sure if
  implemented.
- `objectives` - one or more ScenarioObjectives to be added to the scenario template. Implemented.
