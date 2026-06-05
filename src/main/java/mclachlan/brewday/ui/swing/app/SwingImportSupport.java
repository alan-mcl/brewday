package mclachlan.brewday.ui.swing.app;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.WaterParameters;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

public final class SwingImportSupport
{
	private SwingImportSupport()
	{
	}

	public enum Bit
	{
		WATER_NEW, WATER_UPDATE,
		FERMENTABLE_NEW, FERMENTABLE_UPDATE,
		HOPS_NEW, HOPS_UPDATE,
		YEASTS_NEW, YEASTS_UPDATE,
		MISC_NEW, MISC_UPDATE,
		STYLE_NEW, STYLE_UPDATE,
		EQUIPMENT_NEW, EQUIPMENT_UPDATE,
		RECIPE_NEW, RECIPE_UPDATE,
		BATCH_NEW, BATCH_UPDATE,
		PROCESS_TEMPLATE_NEW, PROCESS_TEMPLATE_UPDATE,
		WATER_PARAMETERS_NEW, WATER_PARAMETERS_UPDATE,
		INVENTORY_NEW, INVENTORY_UPDATE
	}

	public static final class ProcessTemplateMarker
	{
		private ProcessTemplateMarker()
		{
		}
	}

	public static final class ImportSummary
	{
		private int added;
		private int updated;
		private final List<String> lines = new ArrayList<>();

		public int getAdded()
		{
			return added;
		}

		public int getUpdated()
		{
			return updated;
		}

		public List<String> getLines()
		{
			return lines;
		}
	}

	public static final class ImportSelection
	{
		private final Map<Class<?>, Map<String, V2DataObject>> imported;
		private final BitSet options;
		private final List<String> migrationWarnings;

		public ImportSelection(Map<Class<?>, Map<String, V2DataObject>> imported, BitSet options)
		{
			this(imported, options, List.of());
		}

		public ImportSelection(
			Map<Class<?>, Map<String, V2DataObject>> imported,
			BitSet options,
			List<String> migrationWarnings)
		{
			this.imported = imported;
			this.options = options;
			this.migrationWarnings = migrationWarnings == null ? List.of() : List.copyOf(migrationWarnings);
		}

		public Map<Class<?>, Map<String, V2DataObject>> getImported()
		{
			return imported;
		}

		public BitSet getOptions()
		{
			return options;
		}

		public List<String> getMigrationWarnings()
		{
			return migrationWarnings;
		}
	}

	public static final class EntityOption
	{
		private final String label;
		private final int newCount;
		private final int updateCount;
		private final Bit newBit;
		private final Bit updateBit;

		public EntityOption(String label, int newCount, int updateCount, Bit newBit, Bit updateBit)
		{
			this.label = label;
			this.newCount = newCount;
			this.updateCount = updateCount;
			this.newBit = newBit;
			this.updateBit = updateBit;
		}

		public String getLabel()
		{
			return label;
		}

		public int getNewCount()
		{
			return newCount;
		}

		public int getUpdateCount()
		{
			return updateCount;
		}

		public Bit getNewBit()
		{
			return newBit;
		}

		public Bit getUpdateBit()
		{
			return updateBit;
		}
	}

	public static ImportSummary applyImport(
		Map<Class<?>, Map<String, V2DataObject>> imported,
		BitSet options,
		DirtyStateService dirtyState)
	{
		ImportSummary summary = new ImportSummary();
		Database db = Database.getInstance();

		merge(imported.get(Water.class), db.getWaters(), options, Bit.WATER_NEW, Bit.WATER_UPDATE, dirtyState, "water", summary,
			"Water");
		merge(imported.get(Fermentable.class), db.getFermentables(), options, Bit.FERMENTABLE_NEW, Bit.FERMENTABLE_UPDATE, dirtyState,
			"fermentables", summary, "Fermentables");
		merge(imported.get(Hop.class), db.getHops(), options, Bit.HOPS_NEW, Bit.HOPS_UPDATE, dirtyState, "hops", summary, "Hops");
		merge(imported.get(Yeast.class), db.getYeasts(), options, Bit.YEASTS_NEW, Bit.YEASTS_UPDATE, dirtyState, "yeast", summary,
			"Yeast");
		merge(imported.get(Misc.class), db.getMiscs(), options, Bit.MISC_NEW, Bit.MISC_UPDATE, dirtyState, "misc", summary, "Misc");
		merge(imported.get(Style.class), db.getStyles(), options, Bit.STYLE_NEW, Bit.STYLE_UPDATE, dirtyState, "styles", summary,
			"Styles");
		merge(imported.get(EquipmentProfile.class), db.getEquipmentProfiles(), options, Bit.EQUIPMENT_NEW, Bit.EQUIPMENT_UPDATE,
			dirtyState, "equipment.profiles", summary, "Equipment Profiles");
		merge(imported.get(WaterParameters.class), db.getWaterParameters(), options, Bit.WATER_PARAMETERS_NEW, Bit.WATER_PARAMETERS_UPDATE,
			dirtyState, "water.parameters", summary, "Water Parameters");
		merge(imported.get(ProcessTemplateMarker.class), db.getProcessTemplates(), options, Bit.PROCESS_TEMPLATE_NEW,
			Bit.PROCESS_TEMPLATE_UPDATE, dirtyState, "processTemplates", summary, "Process Templates");
		merge(imported.get(InventoryLineItem.class), db.getInventory(), options, Bit.INVENTORY_NEW, Bit.INVENTORY_UPDATE, dirtyState,
			"inventory", summary, "Inventory");
		merge(imported.get(Recipe.class), db.getRecipes(), options, Bit.RECIPE_NEW, Bit.RECIPE_UPDATE, dirtyState, "recipes", summary,
			"Recipes");
		merge(imported.get(Batch.class), db.getBatches(), options, Bit.BATCH_NEW, Bit.BATCH_UPDATE, dirtyState, "batches", summary,
			"Batches");

		if (summary.getAdded() > 0 || summary.getUpdated() > 0)
		{
			dirtyState.markDirty("brewing", "reference.database");
		}

		return summary;
	}

	public static List<EntityOption> buildEntityOptions(Map<Class<?>, Map<String, V2DataObject>> imported)
	{
		List<EntityOption> result = new ArrayList<>();
		Database db = Database.getInstance();
		addOption(result, "Water", imported.get(Water.class), db.getWaters(), Bit.WATER_NEW, Bit.WATER_UPDATE);
		addOption(result, "Fermentables", imported.get(Fermentable.class), db.getFermentables(), Bit.FERMENTABLE_NEW,
			Bit.FERMENTABLE_UPDATE);
		addOption(result, "Hops", imported.get(Hop.class), db.getHops(), Bit.HOPS_NEW, Bit.HOPS_UPDATE);
		addOption(result, "Yeast", imported.get(Yeast.class), db.getYeasts(), Bit.YEASTS_NEW, Bit.YEASTS_UPDATE);
		addOption(result, "Misc", imported.get(Misc.class), db.getMiscs(), Bit.MISC_NEW, Bit.MISC_UPDATE);
		addOption(result, "Styles", imported.get(Style.class), db.getStyles(), Bit.STYLE_NEW, Bit.STYLE_UPDATE);
		addOption(result, "Equipment Profiles", imported.get(EquipmentProfile.class), db.getEquipmentProfiles(), Bit.EQUIPMENT_NEW,
			Bit.EQUIPMENT_UPDATE);
		addOption(result, "Water Parameters", imported.get(WaterParameters.class), db.getWaterParameters(), Bit.WATER_PARAMETERS_NEW,
			Bit.WATER_PARAMETERS_UPDATE);
		addOption(result, "Process Templates", imported.get(ProcessTemplateMarker.class), db.getProcessTemplates(), Bit.PROCESS_TEMPLATE_NEW,
			Bit.PROCESS_TEMPLATE_UPDATE);
		addOption(result, "Inventory", imported.get(InventoryLineItem.class), db.getInventory(), Bit.INVENTORY_NEW, Bit.INVENTORY_UPDATE);
		addOption(result, "Recipes", imported.get(Recipe.class), db.getRecipes(), Bit.RECIPE_NEW, Bit.RECIPE_UPDATE);
		addOption(result, "Batches", imported.get(Batch.class), db.getBatches(), Bit.BATCH_NEW, Bit.BATCH_UPDATE);
		return result;
	}

	private static void addOption(
		List<EntityOption> options,
		String label,
		Map<String, V2DataObject> imported,
		Map<String, ?> currentDb,
		Bit newBit,
		Bit updateBit)
	{
		if (imported == null || imported.isEmpty())
		{
			return;
		}

		int newCount = 0;
		int updateCount = 0;
		for (V2DataObject obj : imported.values())
		{
			if (currentDb.containsKey(obj.getName()))
			{
				updateCount++;
			}
			else
			{
				newCount++;
			}
		}
		options.add(new EntityOption(label, newCount, updateCount, newBit, updateBit));
	}

	private static void merge(
		Map<String, V2DataObject> imported,
		Map currentDb,
		BitSet options,
		Bit newBit,
		Bit updateBit,
		DirtyStateService dirtyState,
		String dirtyToken,
		ImportSummary summary,
		String label)
	{
		if (imported == null || imported.isEmpty())
		{
			return;
		}

		boolean importNew = options.get(newBit.ordinal());
		boolean importUpdates = options.get(updateBit.ordinal());
		int addCount = 0;
		int updateCount = 0;

		for (Map.Entry<String, V2DataObject> e : imported.entrySet())
		{
			String name = e.getKey();
			V2DataObject value = e.getValue();
			boolean exists = currentDb.containsKey(name);
			if (exists && importUpdates)
			{
				currentDb.put(name, value);
				dirtyState.markDirty(value, dirtyToken);
				updateCount++;
			}
			else if (!exists && importNew)
			{
				currentDb.put(name, value);
				dirtyState.markDirty(value, dirtyToken);
				addCount++;
			}
		}

		if (addCount > 0 || updateCount > 0)
		{
			summary.added += addCount;
			summary.updated += updateCount;
			summary.lines.add(label + ": +" + addCount + " / ~" + updateCount);
		}
	}

	public static Map<Class<?>, Map<String, V2DataObject>> createEmptyImportedMap()
	{
		return new HashMap<>();
	}
}
