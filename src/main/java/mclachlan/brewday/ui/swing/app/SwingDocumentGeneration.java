package mclachlan.brewday.ui.swing.app;

import java.awt.Desktop;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.document.DocumentCreator;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing equivalent of JFX document generation: choose template, save file, create document, open.
 */
public final class SwingDocumentGeneration
{
	private SwingDocumentGeneration()
	{
	}

	/**
	 * @return true if a document was written
	 */
	public static boolean run(Window parent, Recipe recipe)
	{
		if (recipe == null)
		{
			return false;
		}
		List<String> documentTemplates = Database.getInstance().getDocumentTemplates();
		if (documentTemplates == null || documentTemplates.isEmpty())
		{
			SwingUiErrors.showError(parent, getUiString("doc.gen.no.templates"),
				getUiString("doc.gen.generate.document"));
			return false;
		}

		Object chosen = JOptionPane.showInputDialog(parent,
			getUiString("doc.gen.choose.template"),
			getUiString("doc.gen.generate.document"),
			JOptionPane.QUESTION_MESSAGE,
			null,
			documentTemplates.toArray(),
			documentTemplates.get(0));
		if (!(chosen instanceof String template))
		{
			return false;
		}

		String defaultSuffix = template.substring(0, template.indexOf('.'));
		String extension = template.substring(
			template.indexOf('.') + 1,
			template.lastIndexOf('.'));

		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("." + extension, extension));
		String initialName = recipe.getName().replaceAll("\\W", "_") + "_" + defaultSuffix + "." + extension;
		chooser.setSelectedFile(new File(initialName));

		Settings settings = Database.getInstance().getSettings();
		String dirS = settings.get(Settings.LAST_EXPORT_DIRECTORY);
		if (dirS != null)
		{
			File dir = new File(dirS);
			if (dir.isDirectory())
			{
				chooser.setCurrentDirectory(dir);
			}
		}

		if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
		{
			return false;
		}
		File file = chooser.getSelectedFile();
		String parentPath = file.getParent();
		if (parentPath != null)
		{
			settings.set(Settings.LAST_EXPORT_DIRECTORY, parentPath);
			Database.getInstance().saveSettings();
		}

		DocumentCreator dc = DocumentCreator.getInstance();
		try
		{
			dc.createDocument(recipe, template, file);
			if (Desktop.isDesktopSupported())
			{
				Desktop.getDesktop().open(file);
			}
			return true;
		}
		catch (IOException ex)
		{
			throw new BrewdayException(ex);
		}
	}
}
