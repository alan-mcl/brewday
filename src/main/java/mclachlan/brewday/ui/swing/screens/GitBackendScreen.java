package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.backends.git.GitBackend;
import mclachlan.brewday.db.backends.git.GitCommandExecutor;
import mclachlan.brewday.db.backends.git.GitCommandSessionLog;
import mclachlan.brewday.db.backends.git.GitRepoStatus;
import mclachlan.brewday.db.backends.git.GitStatusService;
import mclachlan.brewday.db.backends.git.GitStatusSnapshot;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.dialogs.GitNewBackupSetupDialog;
import mclachlan.brewday.ui.swing.dialogs.GitRestoreSetupDialog;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Git backup settings: two explicit setup workflows and conservative sync.
 */
public class GitBackendScreen extends JPanel implements SwingScreen
{
	private static final int LEFT_FORM_PREF_WIDTH_PX = 500;

	private boolean refreshing;
	private volatile boolean gitTaskRunning;

	private final CardLayout bodyCards = new CardLayout();
	private final JPanel bodyPanel = new JPanel(bodyCards);

	private final JPanel disabledPanel = new JPanel();
	private final JPanel enabledPanel = new JPanel();

	private final JTextArea intro = new JTextArea();
	private final JLabel dbDirLabel = new JLabel();
	private final JButton setupNewButton = new JButton(getUiString("settings.git.setup.new"));
	private final JButton setupRestoreButton = new JButton(getUiString("settings.git.setup.restore"));

	private final JTextArea introEnabled = new JTextArea();
	private final JCheckBox autoPushCheck = new JCheckBox(getUiString("settings.git.auto.push"));
	private final JLabel statusLabel = new JLabel();
	private final JLabel branchLabel = new JLabel();
	private final JLabel remoteUrlLabel = new JLabel();
	private final JButton addRemoteButton = new JButton(getUiString("settings.git.add.remote"));
	private final JButton syncButton = new JButton(getUiString("settings.git.sync.remote"));
	private final JButton disableButton = new JButton(getUiString("settings.git.disable.tracking"));
	private final JButton refreshStatusButton = new JButton(getUiString("settings.git.refresh.status"));
	private final JButton clearLogButton = new JButton(getUiString("settings.git.clear.log"));
	private final JPanel logPanel = new JPanel(new BorderLayout(4, 4));
	private final JTextArea commandLog = new JTextArea();

	public GitBackendScreen()
	{
		super(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));

		buildDisabledPanel();
		buildEnabledPanel();
		bodyPanel.add(disabledPanel, "disabled");
		bodyPanel.add(enabledPanel, "enabled");

		JPanel leftWrap = new JPanel(new BorderLayout(0, 0));
		leftWrap.add(bodyPanel, BorderLayout.NORTH);

		Dimension leftPref = leftWrap.getPreferredSize();
		Dimension cappedLeft = new Dimension(LEFT_FORM_PREF_WIDTH_PX, leftPref.height);
		leftWrap.setPreferredSize(cappedLeft);
		leftWrap.setMinimumSize(new Dimension(
			Math.min(LEFT_FORM_PREF_WIDTH_PX, leftPref.width),
			leftPref.height));

		autoPushCheck.setToolTipText(getUiString("settings.git.auto.push.tooltip"));
		addRemoteButton.setToolTipText(getUiString("settings.git.set.origin.tooltip"));
		syncButton.setToolTipText(getUiString("settings.git.sync.tooltip"));
		refreshStatusButton.setToolTipText(getUiString("settings.git.refresh.status.tooltip"));
		clearLogButton.setToolTipText(getUiString("settings.git.clear.log.tooltip"));

		add(leftWrap, BorderLayout.WEST);
		buildLogPanel();
		add(logPanel, BorderLayout.CENTER);
		refresh();
		wireActions();
	}

	private void buildDisabledPanel()
	{
		disabledPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = gridConstraints();

		intro.setEditable(false);
		intro.setOpaque(false);
		intro.setLineWrap(true);
		intro.setWrapStyleWord(true);
		intro.setText(getUiString("settings.git.intro"));
		intro.setColumns(54);
		intro.setBorder(BorderFactory.createEmptyBorder());
		disabledPanel.add(intro, gbc);

		gbc.gridy++;
		disabledPanel.add(new JLabel(getUiString("settings.git.db.dir.label")), gbc);

		gbc.gridy++;
		dbDirLabel.setFont(dbDirLabel.getFont().deriveFont(Font.PLAIN));
		disabledPanel.add(dbDirLabel, gbc);

		gbc.gridy++;
		gbc.fill = GridBagConstraints.NONE;
		JPanel setupRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		setupRow.add(setupNewButton);
		setupRow.add(setupRestoreButton);
		disabledPanel.add(setupRow, gbc);
	}

	private void buildEnabledPanel()
	{
		enabledPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = gridConstraints();

		introEnabled.setEditable(false);
		introEnabled.setOpaque(false);
		introEnabled.setLineWrap(true);
		introEnabled.setWrapStyleWord(true);
		introEnabled.setText(getUiString("settings.git.intro.enabled"));
		introEnabled.setColumns(54);
		introEnabled.setBorder(BorderFactory.createEmptyBorder());
		enabledPanel.add(introEnabled, gbc);

		gbc.gridy++;
		enabledPanel.add(autoPushCheck, gbc);

		gbc.gridy++;
		enabledPanel.add(new JLabel(getUiString("settings.git.status.label")), gbc);

		gbc.gridy++;
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
		enabledPanel.add(statusLabel, gbc);

		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		enabledPanel.add(branchLabel, gbc);

		gbc.gridy++;
		enabledPanel.add(remoteUrlLabel, gbc);

		gbc.gridy++;
		gbc.fill = GridBagConstraints.NONE;
		JPanel remoteRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		remoteRow.add(addRemoteButton);
		enabledPanel.add(remoteRow, gbc);

		gbc.gridy++;
		JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		actionRow.add(syncButton);
		actionRow.add(refreshStatusButton);
		actionRow.add(disableButton);
		enabledPanel.add(actionRow, gbc);
	}

	private static GridBagConstraints gridConstraints()
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		return gbc;
	}

	private void buildLogPanel()
	{
		JPanel logHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		logHeader.add(new JLabel(getUiString("settings.git.command.log")));
		logHeader.add(clearLogButton);
		logPanel.add(logHeader, BorderLayout.NORTH);

		commandLog.setEditable(false);
		commandLog.setLineWrap(true);
		commandLog.setWrapStyleWord(true);
		commandLog.setRows(12);
		commandLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, commandLog.getFont().getSize()));
		logPanel.add(new JScrollPane(commandLog), BorderLayout.CENTER);
	}

	private Window windowAncestor()
	{
		return SwingUtilities.getWindowAncestor(this);
	}

	private GitBackend.OutputCollector logCollector()
	{
		return GitCommandExecutor.combinedCollector(
			s -> EventQueue.invokeLater(() -> commandLog.append(s)));
	}

	private void logSectionHeader(String title)
	{
		GitBackend.OutputCollector collector = logCollector();
		collector.append(GitCommandSessionLog.formatLogTimestamp());
		collector.append(" ");
		collector.append(title);
		if (!title.endsWith("\n"))
		{
			collector.append("\n");
		}
	}

	private void loadSessionLogIntoView()
	{
		commandLog.setText(GitCommandSessionLog.snapshot());
		commandLog.setCaretPosition(commandLog.getDocument().getLength());
	}

	private void setGitTaskRunning(boolean running)
	{
		this.gitTaskRunning = running;
		updateControlsEnabledState();
	}

	private void updateControlsEnabledState()
	{
		boolean busy = gitTaskRunning;
		boolean idle = !busy;
		boolean gitOn = idle && isGitEnabled();

		setupNewButton.setEnabled(idle && !gitOn);
		setupRestoreButton.setEnabled(idle && !gitOn);

		autoPushCheck.setEnabled(gitOn);
		syncButton.setEnabled(gitOn);
		refreshStatusButton.setEnabled(idle);
		disableButton.setEnabled(gitOn);
		clearLogButton.setEnabled(idle);

		if (gitOn)
		{
			boolean hasOrigin = false;
			try
			{
				GitStatusSnapshot snap = Database.getInstance().getGitStatusSnapshot();
				String remote = snap.getRemoteUrl();
				hasOrigin = remote != null && !remote.isBlank();
			}
			catch (Exception ignored)
			{
				// keep hasOrigin false
			}
			addRemoteButton.setEnabled(!hasOrigin);
		}
		else
		{
			addRemoteButton.setEnabled(false);
		}
	}

	private static boolean isGitEnabled()
	{
		return "true".equalsIgnoreCase(
			Database.getInstance().getSettings().get(Settings.GIT_BACKEND_ENABLED));
	}

	private void runGitBackendTask(Runnable backgroundWork)
	{
		runGitBackendTask(backgroundWork, null);
	}

	private void runGitBackendTask(Runnable backgroundWork, Runnable onSuccess)
	{
		if (gitTaskRunning)
		{
			return;
		}
		setGitTaskRunning(true);
		SwingWorker<Void, Void> worker = new SwingWorker<>()
		{
			@Override
			protected Void doInBackground()
			{
				backgroundWork.run();
				return null;
			}

			@Override
			protected void done()
			{
				setGitTaskRunning(false);
				try
				{
					get();
					if (onSuccess != null)
					{
						onSuccess.run();
					}
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
				catch (ExecutionException e)
				{
					Throwable c = e.getCause();
					if (c instanceof BrewdayException brewdayException)
					{
						appendError(brewdayException);
					}
					else if (c instanceof RuntimeException re && re.getCause() != null)
					{
						appendError(re.getCause());
					}
					else
					{
						appendError(c != null ? c : e);
					}
				}
				refresh();
			}
		};
		worker.execute();
	}

	private void appendError(Throwable t)
	{
		if (t == null)
		{
			return;
		}
		String summary = t.getMessage();
		StringWriter sw = new StringWriter();
		if (summary != null && !summary.isBlank())
		{
			sw.append(summary);
			if (!summary.endsWith("\n"))
			{
				sw.append('\n');
			}
			sw.append('\n');
		}
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		String full = sw.toString();
		EventQueue.invokeLater(() ->
		{
			commandLog.append(full);
			if (!full.endsWith("\n"))
			{
				commandLog.append("\n");
			}
		});
	}

	private void showRestartRequired()
	{
		JOptionPane.showMessageDialog(
			windowAncestor(),
			getUiString("settings.git.restart.required.msg"),
			getUiString("settings.git.restart.required.title"),
			JOptionPane.INFORMATION_MESSAGE);
	}

	private void wireActions()
	{
		setupNewButton.addActionListener(e ->
		{
			GitNewBackupSetupDialog.Result wizard = new GitNewBackupSetupDialog(
				windowAncestor()).showDialog();
			if (!wizard.isApproved())
			{
				return;
			}
			String remote = wizard.isLocalOnly() ? null : wizard.getRemoteUrl();
			runGitBackendTask(() ->
			{
				logSectionHeader("--- Set up Git backup ---");
				Database.getInstance().setupNewGitBackup(remote, logCollector());
			});
		});

		setupRestoreButton.addActionListener(e ->
		{
			GitRestoreSetupDialog.Result wizard = new GitRestoreSetupDialog(
				windowAncestor()).showDialog();
			if (wizard.getAction() == GitRestoreSetupDialog.Action.CANCELLED)
			{
				return;
			}
			if (wizard.getAction() == GitRestoreSetupDialog.Action.ADOPT_FOLDER)
			{
				runGitBackendTask(() ->
				{
					logSectionHeader("--- Adopt repository ---");
					boolean restart = Database.getInstance().adoptGitRepositoryAt(
						wizard.getFolder(), logCollector());
					if (restart)
					{
						EventQueue.invokeLater(this::showRestartRequired);
					}
				});
			}
			else
			{
				runGitBackendTask(() ->
				{
					logSectionHeader("--- Clone repository ---");
					boolean restart = Database.getInstance().cloneGitRepository(
						wizard.getRemoteUrl(),
						wizard.getCloneDestination(),
						logCollector());
					if (restart)
					{
						EventQueue.invokeLater(this::showRestartRequired);
					}
				});
			}
		});

		autoPushCheck.addActionListener(e ->
		{
			if (refreshing || !isGitEnabled())
			{
				return;
			}
			Settings settings = Database.getInstance().getSettings();
			settings.set(Settings.GIT_AUTO_PUSH, autoPushCheck.isSelected() ? "true" : "false");
			Database.getInstance().saveSettings(true);
		});

		addRemoteButton.addActionListener(e -> runAddRemoteBackup());

		syncButton.addActionListener(e ->
		{
			GitStatusSnapshot snap = Database.getInstance().getGitStatusSnapshot();
			if (snap.getStatus() == GitRepoStatus.Diverged
				|| snap.getStatus() == GitRepoStatus.PullRequired
				|| snap.getStatus() == GitRepoStatus.Uncommitted)
			{
				int opt = JOptionPane.showConfirmDialog(
					windowAncestor(),
					getUiString("settings.git.sync.tooltip"),
					getUiString("settings.git.sync.remote"),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);
				if (opt != JOptionPane.OK_OPTION)
				{
					return;
				}
			}
			runGitBackendTask(() ->
			{
				logSectionHeader("--- Sync ---");
				Database.getInstance().syncGitRemoteSafely(logCollector());
			});
		});

		disableButton.addActionListener(e ->
		{
			int opt = JOptionPane.showConfirmDialog(
				windowAncestor(),
				getUiString("settings.git.disable.dialog.text"),
				getUiString("settings.git.disable.dialog.title"),
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if (opt != JOptionPane.OK_OPTION)
			{
				return;
			}
			runGitBackendTask(() ->
			{
				logSectionHeader("--- Disable Git tracking ---");
				Database.getInstance().disableGitBackend(logCollector());
			});
		});

		refreshStatusButton.addActionListener(e ->
			runGitBackendTask(() ->
			{
				logSectionHeader("--- Refresh status ---");
				applyStatusSnapshot(Database.getInstance().getGitStatusSnapshot(logCollector()));
			}));

		clearLogButton.addActionListener(e ->
		{
			GitCommandSessionLog.clear();
			commandLog.setText("");
		});
	}

	private void runAddRemoteBackup()
	{
		if (!isGitEnabled())
		{
			return;
		}

		String url = JOptionPane.showInputDialog(
			windowAncestor(),
			getUiString("settings.git.set.origin.prompt"),
			getUiString("settings.git.add.remote"),
			JOptionPane.QUESTION_MESSAGE);
		if (url == null || url.isBlank())
		{
			return;
		}

		String trimmed = url.trim();
		runGitBackendTask(() ->
		{
			logSectionHeader("--- Add remote backup ---");
			Database.getInstance().addGitRemoteBackup(trimmed, logCollector());
		});
	}

	private void applyStatusSnapshot(GitStatusSnapshot snapshot)
	{
		EventQueue.invokeLater(() ->
		{
			GitRepoStatus status = snapshot.getStatus();
			String statusText = getUiString(GitStatusService.statusMessageKey(status));
			if (snapshot.getAhead() > 0 || snapshot.getBehind() > 0)
			{
				statusText = statusText + " (" + snapshot.getAhead() + " ahead, "
					+ snapshot.getBehind() + " behind)";
			}
			statusLabel.setText(statusText);

			String branch = snapshot.getBranch();
			branchLabel.setText(getUiString("settings.git.branch.label")
				+ " " + (branch != null && !branch.isBlank() ? branch : "—"));

			String remote = snapshot.getRemoteUrl();
			remoteUrlLabel.setText(getUiString("settings.git.remote.url.readonly")
				+ " " + (remote != null && !remote.isBlank() ? remote : getUiString("settings.git.no.origin")));
		});
	}

	private void refreshStatusDisplay()
	{
		try
		{
			applyStatusSnapshot(Database.getInstance().getGitStatusSnapshot());
		}
		catch (Exception ex)
		{
			EventQueue.invokeLater(() ->
			{
				statusLabel.setText(getUiString("settings.git.status.RepoError"));
				branchLabel.setText("");
				remoteUrlLabel.setText("");
			});
		}
	}

	@Override
	public void refresh()
	{
		refreshing = true;
		try
		{
			if (!gitTaskRunning)
			{
				boolean enabled = isGitEnabled();
				bodyCards.show(bodyPanel, enabled ? "enabled" : "disabled");

				dbDirLabel.setText(Database.getInstance().getLocalStorageDirectory().getAbsolutePath());

				if (enabled)
				{
					autoPushCheck.setSelected(
						"true".equalsIgnoreCase(
							Database.getInstance().getSettings().get(Settings.GIT_AUTO_PUSH)));
				}
			}

			loadSessionLogIntoView();
			refreshStatusDisplay();
		}
		finally
		{
			refreshing = false;
		}
		updateControlsEnabledState();
	}
}
