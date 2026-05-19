package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.backends.git.GitRemoteTestResult;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Workflow 1: new git backup setup wizard.
 */
public class GitNewBackupSetupDialog extends JDialog
{
	public static final class Result
	{
		private final boolean approved;
		private final boolean localOnly;
		private final String remoteUrl;

		private Result(boolean approved, boolean localOnly, String remoteUrl)
		{
			this.approved = approved;
			this.localOnly = localOnly;
			this.remoteUrl = remoteUrl;
		}

		public static Result cancelled()
		{
			return new Result(false, true, null);
		}

		public static Result localOnly()
		{
			return new Result(true, true, null);
		}

		public static Result withRemote(String remoteUrl)
		{
			return new Result(true, false, remoteUrl);
		}

		public boolean isApproved()
		{
			return approved;
		}

		public boolean isLocalOnly()
		{
			return localOnly;
		}

		public String getRemoteUrl()
		{
			return remoteUrl;
		}
	}

	private final CardLayout cards = new CardLayout();
	private final JPanel cardPanel = new JPanel(cards);

	private final JRadioButton modeLocal = new JRadioButton(getUiString("settings.git.setup.mode.local"), true);
	private final JRadioButton modeRemote = new JRadioButton(getUiString("settings.git.setup.mode.remote"));

	private final JTextField remoteUrlField = new JTextField(42);
	private final JLabel remoteTestLabel = new JLabel(" ");

	private final JButton backButton = new JButton(getUiString("settings.git.wizard.back"));
	private final JButton nextButton = new JButton(getUiString("settings.git.wizard.next"));
	private final JButton finishButton = new JButton(getUiString("settings.git.wizard.finish"));
	private final JButton cancelButton = new JButton(getUiString("ui.cancel"));

	private int step;
	private Result result = Result.cancelled();
	private GitRemoteTestResult lastRemoteTest;

	public GitNewBackupSetupDialog(Window owner)
	{
		super(owner, getUiString("settings.git.setup.new.title"), ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(8, 8));

		buildCards();
		add(cardPanel, BorderLayout.CENTER);
		add(buildButtonBar(), BorderLayout.SOUTH);

		ButtonGroup group = new ButtonGroup();
		group.add(modeLocal);
		group.add(modeRemote);

		wireActions();
		showStep(0);
		setSize(620, 420);
		setLocationRelativeTo(owner);
	}

	private void buildCards()
	{
		cardPanel.add(buildModeCard(), "mode");
		cardPanel.add(buildLocalConfirmCard(), "localConfirm");
		cardPanel.add(buildRemoteCard(), "remote");
	}

	private JPanel buildModeCard()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		panel.add(modeLocal, gbc);
		gbc.gridy++;
		JTextArea localDesc = readOnlyArea(getUiString("settings.git.setup.mode.local.desc"));
		panel.add(localDesc, gbc);
		gbc.gridy++;
		panel.add(modeRemote, gbc);
		gbc.gridy++;
		JTextArea remoteDesc = readOnlyArea(getUiString("settings.git.setup.mode.remote.desc"));
		panel.add(remoteDesc, gbc);
		return panel;
	}

	private JPanel buildLocalConfirmCard()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		panel.add(readOnlyArea(getUiString("settings.git.setup.confirm.local")), BorderLayout.NORTH);
		return panel;
	}

	private JPanel buildRemoteCard()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(4, 4, 4, 4);
		panel.add(new JLabel(getUiString("settings.git.remote.url.label")), gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		remoteUrlField.setText("");
		panel.add(remoteUrlField, gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.NONE;
		JButton testButton = new JButton(getUiString("settings.git.remote.test"));
		testButton.addActionListener(e -> runRemoteTest());
		panel.add(testButton, gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(remoteTestLabel, gbc);
		return panel;
	}

	private JPanel buildButtonBar()
	{
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		DialogButtonTooltips.wireOkCancel(finishButton, cancelButton);
		bar.add(backButton);
		bar.add(nextButton);
		bar.add(finishButton);
		bar.add(cancelButton);
		return bar;
	}

	private static JTextArea readOnlyArea(String text)
	{
		JTextArea area = new JTextArea(text);
		area.setEditable(false);
		area.setOpaque(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setColumns(52);
		return area;
	}

	private void wireActions()
	{
		cancelButton.addActionListener(e -> dispose());
		backButton.addActionListener(e ->
		{
			if (step > 0)
			{
				showStep(step - 1);
			}
		});
		nextButton.addActionListener(e -> advance());
		finishButton.addActionListener(e -> finishWizard());
	}

	private void advance()
	{
		if (step == 0)
		{
			if (modeLocal.isSelected())
			{
				showStep(1);
			}
			else
			{
				showStep(2);
			}
			return;
		}
		finishWizard();
	}

	private void finishWizard()
	{
		if (modeLocal.isSelected() || step == 1)
		{
			result = Result.localOnly();
			dispose();
			return;
		}

		String url = remoteUrlField.getText().trim();
		if (url.isEmpty())
		{
			remoteTestLabel.setText(getUiString("settings.git.remote.url.label"));
			return;
		}
		if (lastRemoteTest != GitRemoteTestResult.OkEmpty)
		{
			runRemoteTest();
			if (lastRemoteTest != GitRemoteTestResult.OkEmpty)
			{
				return;
			}
		}
		result = Result.withRemote(url);
		dispose();
	}

	private void runRemoteTest()
	{
		String url = remoteUrlField.getText().trim();
		if (url.isEmpty())
		{
			remoteTestLabel.setText(" ");
			lastRemoteTest = null;
			return;
		}

		remoteTestLabel.setText("…");
		SwingWorker<GitRemoteTestResult, Void> worker = new SwingWorker<>()
		{
			@Override
			protected GitRemoteTestResult doInBackground()
			{
				return Database.getInstance().testGitRemoteConnectivity(url, s -> {});
			}

			@Override
			protected void done()
			{
				try
				{
					lastRemoteTest = get();
					remoteTestLabel.setText(remoteTestMessage(lastRemoteTest));
					updateButtons();
				}
				catch (Exception ex)
				{
					lastRemoteTest = GitRemoteTestResult.Unreachable;
					remoteTestLabel.setText(getUiString("settings.git.remote.test.unreachable"));
				}
			}
		};
		worker.execute();
	}

	private static String remoteTestMessage(GitRemoteTestResult test)
	{
		return switch (test)
		{
			case OkEmpty -> getUiString("settings.git.remote.test.empty");
			case OkHasCommits -> getUiString("settings.git.remote.test.has.commits");
			case AuthFailed -> getUiString("settings.git.remote.test.auth.failed");
			case Unreachable -> getUiString("settings.git.remote.test.unreachable");
		};
	}

	private void showStep(int newStep)
	{
		step = newStep;
		switch (step)
		{
			case 0 -> cards.show(cardPanel, "mode");
			case 1 -> cards.show(cardPanel, "localConfirm");
			case 2 -> cards.show(cardPanel, "remote");
			default -> cards.show(cardPanel, "mode");
		}
		updateButtons();
	}

	private void updateButtons()
	{
		backButton.setVisible(step > 0);
		boolean remoteStep = step == 2;
		nextButton.setVisible(step == 0 || step == 1);
		finishButton.setVisible(remoteStep);
		if (remoteStep)
		{
			finishButton.setEnabled(lastRemoteTest == GitRemoteTestResult.OkEmpty);
		}
	}

	public Result showDialog()
	{
		setVisible(true);
		return result;
	}
}
