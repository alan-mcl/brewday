package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.backends.git.GitRemoteTestResult;
import mclachlan.brewday.db.backends.git.GitRepositoryAdoptionValidator;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Workflow 2: adopt local folder or clone remote repository.
 */
public class GitRestoreSetupDialog extends JDialog
{
	public enum Action
	{
		CANCELLED,
		ADOPT_FOLDER,
		CLONE_REMOTE
	}

	public static final class Result
	{
		private final Action action;
		private final File folder;
		private final String remoteUrl;
		private final File cloneDestination;

		private Result(Action action, File folder, String remoteUrl, File cloneDestination)
		{
			this.action = action;
			this.folder = folder;
			this.remoteUrl = remoteUrl;
			this.cloneDestination = cloneDestination;
		}

		public static Result cancelled()
		{
			return new Result(Action.CANCELLED, null, null, null);
		}

		public static Result adoptFolder(File folder)
		{
			return new Result(Action.ADOPT_FOLDER, folder, null, null);
		}

		public static Result cloneRemote(String remoteUrl, File cloneDestination)
		{
			return new Result(Action.CLONE_REMOTE, null, remoteUrl, cloneDestination);
		}

		public Action getAction()
		{
			return action;
		}

		public File getFolder()
		{
			return folder;
		}

		public String getRemoteUrl()
		{
			return remoteUrl;
		}

		public File getCloneDestination()
		{
			return cloneDestination;
		}
	}

	private final CardLayout cards = new CardLayout();
	private final JPanel cardPanel = new JPanel(cards);

	private final JRadioButton modeLocal = new JRadioButton(getUiString("settings.git.restore.mode.local"), true);
	private final JRadioButton modeClone = new JRadioButton(getUiString("settings.git.restore.mode.clone"));

	private final JLabel selectedFolderLabel = new JLabel(" ");
	private final JLabel validationLabel = new JLabel(" ");
	private final JCheckBox dirtyConfirmCheck = new JCheckBox(
		getUiString("settings.git.adopt.dirty.confirm"));
	private File selectedFolder;

	private final JTextField remoteUrlField = new JTextField(42);
	private final JLabel remoteTestLabel = new JLabel(" ");
	private final JLabel cloneDestLabel = new JLabel(" ");
	private File cloneDestination;
	private GitRemoteTestResult lastRemoteTest;

	private final JButton backButton = new JButton(getUiString("settings.git.wizard.back"));
	private final JButton nextButton = new JButton(getUiString("settings.git.wizard.next"));
	private final JButton finishButton = new JButton(getUiString("settings.git.wizard.finish"));
	private final JButton cancelButton = new JButton(getUiString("ui.cancel"));

	private int step;
	private Result result = Result.cancelled();
	private GitRepositoryAdoptionValidator.Result lastValidation;

	public GitRestoreSetupDialog(Window owner)
	{
		super(owner, getUiString("settings.git.setup.restore.title"), ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(8, 8));

		buildCards();
		add(cardPanel, BorderLayout.CENTER);
		add(buildButtonBar(), BorderLayout.SOUTH);

		ButtonGroup group = new ButtonGroup();
		group.add(modeLocal);
		group.add(modeClone);

		wireActions();
		showStep(0);
		setSize(640, 440);
		setLocationRelativeTo(owner);
	}

	private void buildCards()
	{
		cardPanel.add(buildModeCard(), "mode");
		cardPanel.add(buildLocalCard(), "local");
		cardPanel.add(buildCloneCard(), "clone");
	}

	private JPanel buildModeCard()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(6, 4, 6, 4);
		panel.add(modeLocal, gbc);
		gbc.gridy++;
		panel.add(modeClone, gbc);
		return panel;
	}

	private JPanel buildLocalCard()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		JButton choose = new JButton(getUiString("settings.git.restore.choose.folder"));
		choose.addActionListener(e -> chooseFolder());
		panel.add(choose, gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		panel.add(selectedFolderLabel, gbc);
		gbc.gridy++;
		panel.add(validationLabel, gbc);
		gbc.gridy++;
		dirtyConfirmCheck.setVisible(false);
		panel.add(dirtyConfirmCheck, gbc);
		return panel;
	}

	private JPanel buildCloneCard()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;

		panel.add(new JLabel(getUiString("settings.git.remote.url.label")), gbc);
		gbc.gridy++;
		panel.add(remoteUrlField, gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.NONE;
		JButton testButton = new JButton(getUiString("settings.git.remote.test"));
		testButton.addActionListener(e -> runRemoteTest());
		panel.add(testButton, gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(remoteTestLabel, gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.NONE;
		JButton chooseDest = new JButton(getUiString("settings.git.restore.choose.dest"));
		chooseDest.addActionListener(e -> chooseCloneDestination());
		panel.add(chooseDest, gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(cloneDestLabel, gbc);
		gbc.gridy++;
		JTextArea confirm = readOnlyArea(getUiString("settings.git.restore.clone.confirm"));
		panel.add(confirm, gbc);
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

	private void chooseFolder()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle(getUiString("settings.git.restore.choose.folder"));
		if (selectedFolder != null)
		{
			chooser.setCurrentDirectory(selectedFolder);
		}
		else
		{
			chooser.setCurrentDirectory(Database.getInstance().getLocalStorageDirectory());
		}
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			selectedFolder = chooser.getSelectedFile();
			selectedFolderLabel.setText(selectedFolder.getAbsolutePath());
			validateSelectedFolder();
		}
	}

	private void validateSelectedFolder()
	{
		if (selectedFolder == null)
		{
			lastValidation = null;
			validationLabel.setText(" ");
			dirtyConfirmCheck.setVisible(false);
			updateButtons();
			return;
		}

		SwingWorker<GitRepositoryAdoptionValidator.Result, Void> worker = new SwingWorker<>()
		{
			@Override
			protected GitRepositoryAdoptionValidator.Result doInBackground() throws Exception
			{
				return GitRepositoryAdoptionValidator.validate(selectedFolder);
			}

			@Override
			protected void done()
			{
				try
				{
					lastValidation = get();
					if (lastValidation.isValid())
					{
						validationLabel.setText(getUiString("settings.git.restore.validation.ok"));
						if (lastValidation.isDirty())
						{
							dirtyConfirmCheck.setVisible(true);
							dirtyConfirmCheck.setSelected(false);
						}
						else
						{
							dirtyConfirmCheck.setVisible(false);
							dirtyConfirmCheck.setSelected(true);
						}
					}
					else
					{
						validationLabel.setText(lastValidation.getMessage());
						dirtyConfirmCheck.setVisible(false);
						dirtyConfirmCheck.setSelected(false);
					}
				}
				catch (Exception ex)
				{
					lastValidation = null;
					validationLabel.setText(ex.getMessage() != null ? ex.getMessage() : ex.toString());
					dirtyConfirmCheck.setVisible(false);
				}
				updateButtons();
			}
		};
		worker.execute();
	}

	private void chooseCloneDestination()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle(getUiString("settings.git.restore.choose.dest"));
		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			cloneDestination = chooser.getSelectedFile();
			cloneDestLabel.setText(cloneDestination.getAbsolutePath());
			updateButtons();
		}
	}

	private void runRemoteTest()
	{
		String url = remoteUrlField.getText().trim();
		if (url.isEmpty())
		{
			remoteTestLabel.setText(" ");
			lastRemoteTest = null;
			updateButtons();
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
				}
				catch (Exception ex)
				{
					lastRemoteTest = GitRemoteTestResult.Unreachable;
					remoteTestLabel.setText(getUiString("settings.git.remote.test.unreachable"));
				}
				updateButtons();
			}
		};
		worker.execute();
	}

	private static String remoteTestMessage(GitRemoteTestResult test)
	{
		return switch (test)
		{
			case OkEmpty -> getUiString("settings.git.clone.remote.empty");
			case OkHasCommits -> getUiString("settings.git.remote.test.has.commits.reachable");
			case AuthFailed -> getUiString("settings.git.remote.test.auth.failed");
			case Unreachable -> getUiString("settings.git.remote.test.unreachable");
		};
	}

	private void advance()
	{
		if (step == 0)
		{
			showStep(modeLocal.isSelected() ? 1 : 2);
		}
	}

	private void finishWizard()
	{
		if (step == 1)
		{
			if (selectedFolder == null || lastValidation == null || !lastValidation.isValid())
			{
				return;
			}
			if (lastValidation.isDirty() && !dirtyConfirmCheck.isSelected())
			{
				return;
			}
			result = Result.adoptFolder(selectedFolder);
			dispose();
			return;
		}

		if (step == 2)
		{
			String url = remoteUrlField.getText().trim();
			if (url.isEmpty() || cloneDestination == null)
			{
				return;
			}
			if (lastRemoteTest != GitRemoteTestResult.OkHasCommits)
			{
				runRemoteTest();
				if (lastRemoteTest != GitRemoteTestResult.OkHasCommits)
				{
					return;
				}
			}
			result = Result.cloneRemote(url, cloneDestination);
			dispose();
		}
	}

	private void showStep(int newStep)
	{
		step = newStep;
		switch (step)
		{
			case 0 -> cards.show(cardPanel, "mode");
			case 1 -> cards.show(cardPanel, "local");
			case 2 -> cards.show(cardPanel, "clone");
			default -> cards.show(cardPanel, "mode");
		}
		updateButtons();
	}

	private void updateButtons()
	{
		backButton.setVisible(step > 0);
		nextButton.setVisible(step == 0);
		finishButton.setVisible(step == 1 || step == 2);

		if (step == 1)
		{
			boolean ok = selectedFolder != null
				&& lastValidation != null
				&& lastValidation.isValid()
				&& (!lastValidation.isDirty() || dirtyConfirmCheck.isSelected());
			finishButton.setEnabled(ok);
		}
		else if (step == 2)
		{
			finishButton.setEnabled(
				cloneDestination != null
					&& !remoteUrlField.getText().trim().isEmpty()
					&& lastRemoteTest == GitRemoteTestResult.OkHasCommits);
		}
	}

	public Result showDialog()
	{
		setVisible(true);
		return result;
	}
}
