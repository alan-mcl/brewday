package mclachlan.brewday.ui.swing.dialogs;

import java.awt.Frame;
import java.io.File;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import mclachlan.brewday.Settings;
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
import mclachlan.brewday.ui.swing.app.SwingImportSupport;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.util.FixStyles;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingImportBrewdayDialog
{
	private final Frame owner;

	public SwingImportBrewdayDialog(Frame owner)
	{
		this.owner = owner;
	}

	public SwingImportSupport.ImportSelection showDialog()
	{
		File directory = chooseDirectory();
		if (directory == null)
		{
			return null;
		}

		SwingImportProgressDialog progress = new SwingImportProgressDialog(owner, getUiString("tools.import.brewday"),
			getUiString("tools.import.parse"));
		SwingWorker<Map<Class<?>, Map<String, V2DataObject>>, Void> worker = new SwingWorker<>()
		{
			@Override
			protected Map<Class<?>, Map<String, V2DataObject>> doInBackground() throws Exception
			{
				return parseDirectory(directory);
			}
		};
		worker.execute();
		worker.addPropertyChangeListener(e ->
		{
			if ("state".equals(e.getPropertyName()) && SwingWorker.StateValue.DONE.equals(e.getNewValue()))
			{
				progress.dispose();
			}
		});
		progress.setVisible(true);

		try
		{
			Map<Class<?>, Map<String, V2DataObject>> imported = worker.get();
			List<SwingImportSupport.EntityOption> entityOptions = SwingImportSupport.buildEntityOptions(imported);
			if (entityOptions.isEmpty())
			{
				JOptionPane.showMessageDialog(owner, "No importable entities found.", getUiString("tools.import.brewday"),
					JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			SwingImportOptionsDialog optionsDialog = new SwingImportOptionsDialog(owner, getUiString("tools.import.brewday"), entityOptions);
			optionsDialog.setVisible(true);
			if (!optionsDialog.isApproved())
			{
				return null;
			}
			BitSet options = optionsDialog.getOptions();
			return new SwingImportSupport.ImportSelection(imported, options);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return null;
		}
		catch (ExecutionException e)
		{
			SwingUiErrors.showError(owner, e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), getUiString("ui.error"));
			return null;
		}
	}

	private Map<Class<?>, Map<String, V2DataObject>> parseDirectory(File directory)
	{
		Database importDb = new Database(directory.getAbsolutePath());
		importDb.loadAll();

		if (importDb.getStyles().containsKey("1A American Light Lager") &&
			!importDb.getStyles().containsKey("1A/American Light Lager/BJCP 2021"))
		{
			new FixStyles().fixStyles(importDb);
		}

		for (Hop hop : importDb.getHops().values())
		{
			if (hop.getForm() == null)
			{
				hop.setForm(Hop.Form.PELLET);
			}
		}

		Map<Class<?>, Map<String, V2DataObject>> imported = new HashMap<>();
		imported.put(Recipe.class, new HashMap<>(importDb.getRecipes()));
		imported.put(Batch.class, new HashMap<>(importDb.getBatches()));
		imported.put(InventoryLineItem.class, new HashMap<>(importDb.getInventory()));
		imported.put(SwingImportSupport.ProcessTemplateMarker.class, new HashMap<>(importDb.getProcessTemplates()));
		imported.put(WaterParameters.class, new HashMap<>(importDb.getWaterParameters()));
		imported.put(EquipmentProfile.class, new HashMap<>(importDb.getEquipmentProfiles()));
		imported.put(Fermentable.class, new HashMap<>(importDb.getFermentables()));
		imported.put(Water.class, new HashMap<>(importDb.getWaters()));
		imported.put(Hop.class, new HashMap<>(importDb.getHops()));
		imported.put(Yeast.class, new HashMap<>(importDb.getYeasts()));
		imported.put(Misc.class, new HashMap<>(importDb.getMiscs()));
		imported.put(Style.class, new HashMap<>(importDb.getStyles()));
		return imported;
	}

	private File chooseDirectory()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(getUiString("tools.import.brewday.title"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);
		Settings settings = Database.getInstance().getSettings();
		String dir = settings.get(Settings.LAST_IMPORT_DIRECTORY);
		if (dir != null)
		{
			File existing = new File(dir);
			if (existing.exists())
			{
				chooser.setCurrentDirectory(existing);
			}
		}
		int result = chooser.showOpenDialog(owner);
		if (result != JFileChooser.APPROVE_OPTION)
		{
			return null;
		}
		File selected = chooser.getSelectedFile();
		if (selected != null)
		{
			settings.set(Settings.LAST_IMPORT_DIRECTORY, selected.getAbsolutePath());
			Database.getInstance().saveSettings();
		}
		return selected;
	}
}
