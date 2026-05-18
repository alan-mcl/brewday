package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.SwingIcons;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code ComputedVolumePane}: shows an output volume name,
 * a leading icon denoting the volume's {@link Volume.Type}, the
 * {@link Volume#describe()} body, and a Rename action that delegates back to
 * the owning step pane via the constructor-supplied callback.
 */
public class SwingComputedVolumePane extends JPanel
{
	private final JLabel typeIconLabel;
	private final JLabel nameLabel;
	private final JLabel bodyLabel;
	private final JButton renameButton;

	private String currentVolName;

	public SwingComputedVolumePane(String title)
	{
		this(title, null);
	}

	public SwingComputedVolumePane(String title, Consumer<String> onRenameRequested)
	{
		super(new BorderLayout(4, 4));
		setBorder(BorderFactory.createTitledBorder(title));

		typeIconLabel = new JLabel();
		typeIconLabel.setPreferredSize(new Dimension(SwingIcons.TOOLBAR_ICON_SIZE, SwingIcons.TOOLBAR_ICON_SIZE));

		nameLabel = new JLabel();
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

		renameButton = new JButton(getUiString("volumes.rename.button"));
		renameButton.setIcon(SwingIcons.toolbarIcon(SwingIcons.IconKey.RENAME));
		renameButton.setToolTipText(getUiString("volumes.rename.button.tooltip"));
		renameButton.setEnabled(false);
		if (onRenameRequested != null)
		{
			renameButton.addActionListener(e ->
			{
				if (currentVolName != null)
				{
					onRenameRequested.accept(currentVolName);
				}
			});
		}
		else
		{
			renameButton.setVisible(false);
		}

		JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		headerLeft.add(typeIconLabel);
		headerLeft.add(nameLabel);

		JPanel header = new JPanel(new BorderLayout(4, 0));
		header.add(headerLeft, BorderLayout.WEST);
		header.add(renameButton, BorderLayout.EAST);

		bodyLabel = new JLabel();
		bodyLabel.setVerticalAlignment(JLabel.TOP);

		add(header, BorderLayout.NORTH);
		add(bodyLabel, BorderLayout.CENTER);
	}

	public void refresh(String volName, Recipe recipe)
	{
		currentVolName = volName;

		if (volName == null || recipe == null || !recipe.getVolumes().contains(volName))
		{
			typeIconLabel.setIcon(null);
			nameLabel.setText(getUiString("volumes.error"));
			renameButton.setEnabled(false);
			String msg = volName == null
				? ""
				: getUiString("volumes.volume.does.not.exist", volName);
			bodyLabel.setText("<html><body style='width:240px'>" + escapeHtml(msg).replace("\n", "<br/>") + "</body></html>");
			return;
		}

		Volume volume = recipe.getVolumes().getVolume(volName);
		Icon icon = SwingIcons.icon(SwingIcons.volumeTypeIcon(volume.getType()), SwingIcons.TOOLBAR_ICON_SIZE);
		typeIconLabel.setIcon(icon);
		nameLabel.setText(volName);
		renameButton.setEnabled(true);

		String desc = volume.describe();
		bodyLabel.setText("<html><body style='width:240px'>" + escapeHtml(desc).replace("\n", "<br/>") + "</body></html>");
	}

	private static String escapeHtml(String s)
	{
		if (s == null)
		{
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/*-------------------------------------------------------------------------*/
	/** Package-local for tests. */

	JLabel getNameLabelForTest()
	{
		return nameLabel;
	}

	JLabel getBodyLabelForTest()
	{
		return bodyLabel;
	}

	JLabel getTypeIconLabelForTest()
	{
		return typeIconLabel;
	}

	JButton getRenameButtonForTest()
	{
		return renameButton;
	}

	String getCurrentVolNameForTest()
	{
		return currentVolName;
	}
}
