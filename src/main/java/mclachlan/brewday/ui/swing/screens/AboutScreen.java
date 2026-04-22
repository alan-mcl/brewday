package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.*;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.ui.swing.app.SwingScreen;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class AboutScreen extends JPanel implements SwingScreen
{
	private final JLabel appField;
	private final JLabel sourceField;
	private final JLabel dbField;
	private final JLabel logField;
	private final JTextArea creditsArea;

	public AboutScreen()
	{
		super(new BorderLayout());
		JPanel details = new JPanel(new GridBagLayout());
		details.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.insets = new Insets(4, 4, 4, 4);

		appField = readOnlyField(getUiString("ui.about.msg", UiUtils.getVersion()), "about.app.version");
		sourceField = readOnlyField(getUiString("ui.about.url"), "about.source.url");
		dbField = readOnlyField(getUiString("ui.about.db", Database.getInstance().getLocalStorageDirectory().getAbsolutePath()), "about.local.db");
		logField = readOnlyField(getUiString("ui.about.log", Brewday.getInstance().getLog().getLogPath()), "about.log.path");
		creditsArea = readOnlyArea(getUiString("ui.about.gpl3") + "\n" + getUiString("ui.about.icons8"), "about.credits");

		details.add(new JLabel(getUiString("ui.about.title")), gbc);
		gbc.gridy++;
		details.add(appField, gbc);
		gbc.gridy++;
		details.add(sourceField, gbc);
		gbc.gridy++;
		details.add(dbField, gbc);
		gbc.gridy++;
		details.add(logField, gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1.0;
		details.add(new JScrollPane(creditsArea), gbc);

		add(details, BorderLayout.CENTER);
	}

	private JLabel readOnlyField(String text, String name)
	{
		JLabel field = new JLabel(text);
		field.setName(name);
		return field;
	}

	private JTextArea readOnlyArea(String text, String name)
	{
		JTextArea area = new JTextArea(text);
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setName(name);
		area.setCaretPosition(0);
		return area;
	}

	JLabel getAppField()
	{
		return appField;
	}

	JLabel getSourceField()
	{
		return sourceField;
	}

	JLabel getDbField()
	{
		return dbField;
	}

	JLabel getLogField()
	{
		return logField;
	}

	JTextArea getCreditsArea()
	{
		return creditsArea;
	}
}
