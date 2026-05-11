package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingImportSupport;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.dialogs.SwingImportBatchesCsvDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingImportBeerXmlDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingImportBrewdayDialog;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class ImportDataScreen extends JPanel implements SwingScreen
{
	private final Frame parent;
	private final DirtyStateService dirtyState;

	public ImportDataScreen(Frame parent, DirtyStateService dirtyState)
	{
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		this.parent = parent;
		this.dirtyState = dirtyState;

		JButton beerXml = new JButton(getUiString("tools.import.beerxml"), SwingIcons.toolbarIcon(SwingIcons.IconKey.IMPORT));
		JButton batchesCsv = new JButton(getUiString("tools.import.batches.csv"), SwingIcons.toolbarIcon(SwingIcons.IconKey.IMPORT));
		JButton brewdayDb = new JButton(getUiString("tools.import.brewday"), SwingIcons.toolbarIcon(SwingIcons.IconKey.BREWDAY));

		JPanel rows = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 8, 8, 8);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridy = 0;
		addImportRow(rows, gbc, beerXml, getUiString("tools.import.beerxml.label"));
		gbc.gridy++;
		addImportRow(rows, gbc, batchesCsv, getUiString("tools.import.batches.csv.label"));
		gbc.gridy++;
		addImportRow(rows, gbc, brewdayDb, getUiString("tools.import.brewday.label"));
		gbc.gridy++;
		gbc.gridx = 0;
		gbc.weighty = 1.0;
		rows.add(new JLabel(), gbc);

		add(rows, BorderLayout.NORTH);

		beerXml.addActionListener(e -> runImport(new SwingImportBeerXmlDialog(parent).showDialog()));
		batchesCsv.addActionListener(e -> runImport(new SwingImportBatchesCsvDialog(parent).showDialog()));
		brewdayDb.addActionListener(e -> runImport(new SwingImportBrewdayDialog(parent).showDialog()));
	}

	private void addImportRow(JPanel panel, GridBagConstraints gbc, JButton button, String labelText)
	{
		gbc.gridx = 0;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		panel.add(button, gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(new JLabel(labelText), gbc);
	}

	private void runImport(SwingImportSupport.ImportSelection selection)
	{
		if (selection == null)
		{
			return;
		}

		SwingImportSupport.ImportSummary summary = SwingImportSupport.applyImport(
			selection.getImported(),
			selection.getOptions(),
			dirtyState);

		if (summary.getAdded() == 0 && summary.getUpdated() == 0)
		{
			JOptionPane.showMessageDialog(parent, getUiString("tools.import.summary.none"), getUiString("tools.import.imported"),
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(getUiString("tools.import.push.ok")).append('\n');
		sb.append(getUiString("tools.import.summary.counts", summary.getAdded(), summary.getUpdated())).append('\n');
		for (String line : summary.getLines())
		{
			sb.append("- ").append(line).append('\n');
		}
		sb.append('\n').append(getUiString("tools.import.then.save"));
		JOptionPane.showMessageDialog(parent, sb.toString(), getUiString("tools.import.imported"), JOptionPane.INFORMATION_MESSAGE);
	}
}
