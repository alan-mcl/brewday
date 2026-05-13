package mclachlan.brewday.ui.swing.dialogs;

import java.awt.Frame;
import java.io.File;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.importexport.csv.BatchesCsvParser;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.SwingImportSupport;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingImportBatchesCsvDialog
{
	private final Frame owner;

	public SwingImportBatchesCsvDialog(Frame owner)
	{
		this.owner = owner;
	}

	public SwingImportSupport.ImportSelection showDialog()
	{
		JComboBox<BatchesCsvParser.CsvFormat> csvFormat = new JComboBox<>(BatchesCsvParser.CsvFormat.values());
		csvFormat.setSelectedItem(BatchesCsvParser.CsvFormat.EXCEL);
		JComboBox<Quantity.Unit> volumeUnit = new JComboBox<>(new Quantity.Unit[] {
			Quantity.Unit.MILLILITRES,
			Quantity.Unit.LITRES,
			Quantity.Unit.US_FLUID_OUNCE,
			Quantity.Unit.US_GALLON
		});
		volumeUnit.setSelectedItem(Quantity.Unit.LITRES);
		JComboBox<Quantity.Unit> densityUnit = new JComboBox<>(new Quantity.Unit[] {
			Quantity.Unit.SPECIFIC_GRAVITY,
			Quantity.Unit.GU,
			Quantity.Unit.PLATO
		});
		densityUnit.setSelectedItem(Quantity.Unit.SPECIFIC_GRAVITY);

		JPanel settingsPanel = new JPanel(new java.awt.GridLayout(3, 2, 6, 6));
		settingsPanel.add(new JLabel(getUiString("tools.import.batches.csv.format")));
		settingsPanel.add(csvFormat);
		settingsPanel.add(new JLabel(getUiString("tools.import.batches.csv.volume.unit")));
		settingsPanel.add(volumeUnit);
		settingsPanel.add(new JLabel(getUiString("tools.import.batches.csv.density.unit")));
		settingsPanel.add(densityUnit);

		int settingsResult = JOptionPane.showConfirmDialog(
			owner,
			settingsPanel,
			getUiString("tools.import.settings"),
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE);
		if (settingsResult != JOptionPane.OK_OPTION)
		{
			return null;
		}

		List<File> files = chooseFiles();
		if (files == null || files.isEmpty())
		{
			return null;
		}

		SwingImportProgressDialog progress = new SwingImportProgressDialog(owner, getUiString("tools.import.batches.csv"),
			getUiString("tools.import.parse"));
		SwingWorker<Map<Class<?>, Map<String, V2DataObject>>, Void> worker = new SwingWorker<>()
		{
			@Override
			protected Map<Class<?>, Map<String, V2DataObject>> doInBackground() throws Exception
			{
				return new BatchesCsvParser().parse(
					files,
					(BatchesCsvParser.CsvFormat)csvFormat.getSelectedItem(),
					(Quantity.Unit)volumeUnit.getSelectedItem(),
					(Quantity.Unit)densityUnit.getSelectedItem());
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
				JOptionPane.showMessageDialog(owner, "No importable entities found.", getUiString("tools.import.batches.csv"),
					JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			SwingImportOptionsDialog optionsDialog = new SwingImportOptionsDialog(owner, getUiString("tools.import.batches.csv"),
				entityOptions);
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
			System.out.println("[Swing import] interrupted");
			return null;
		}
		catch (ExecutionException e)
		{
			Throwable t = e.getCause() != null ? e.getCause() : e;
			SwingUiErrors.showError(owner, t, getUiString("ui.error"));
			return null;
		}
	}

	private List<File> chooseFiles()
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(getUiString("tools.import.batches.csv.title"));
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		Settings settings = Database.getInstance().getSettings();
		String dir = settings.get(Settings.LAST_IMPORT_DIRECTORY);
		if (dir != null)
		{
			File existing = new File(dir);
			if (existing.exists())
			{
				fileChooser.setCurrentDirectory(existing);
			}
		}
		int result = fileChooser.showOpenDialog(owner);
		if (result != JFileChooser.APPROVE_OPTION)
		{
			return null;
		}
		File[] selectedFiles = fileChooser.getSelectedFiles();
		if (selectedFiles == null || selectedFiles.length == 0)
		{
			File one = fileChooser.getSelectedFile();
			if (one == null)
			{
				return null;
			}
			selectedFiles = new File[] { one };
		}
		File parent = selectedFiles[0].getParentFile();
		if (parent != null)
		{
			settings.set(Settings.LAST_IMPORT_DIRECTORY, parent.getAbsolutePath());
			Database.getInstance().saveSettings();
		}
		return List.of(selectedFiles);
	}
}
