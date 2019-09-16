/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import mclachlan.brewday.StringUtils;

/**
 *
 */
public abstract class EditorPanel
	extends JPanel
	implements ListSelectionListener, KeyListener, ActionListener,
	ChangeListener, TableModelListener, ItemListener, MouseListener
{
	public static final String NONE = " - ";

	protected JList names;
	protected Container editControls;
	protected String currentName;

	protected int dirtyFlag;

	/*-------------------------------------------------------------------------*/
	public EditorPanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;
		names = new JList();

		refreshNames(null);
		names.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		names.addListSelectionListener(this);
		JScrollPane nameScroller = new JScrollPane(names);

		setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3,3,3,3);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.NORTHWEST;

		editControls = getEditControls();
		JScrollPane editControlsScroller = new JScrollPane(editControls);

		JSplitPane splitPane = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			true,
			nameScroller,
			editControlsScroller);

		add(splitPane, gbc);

		initForeignKeys();
		if (currentName != null)
		{
			refresh(currentName);
		}

		splitPane.setDividerLocation(-1);
	}

	/*-------------------------------------------------------------------------*/
	public void refreshNames(String toBeSelected)
	{
		currentName = null;
		Vector<String> vec = new Vector<String>(loadData());
		Collections.sort(vec);
		names.setListData(vec);
		if (toBeSelected == null)
		{
			names.setSelectedIndex(0);
		}
		else
		{
			names.setSelectedValue(toBeSelected, true);
		}
		currentName = (String)names.getSelectedValue();
	}

	/*-------------------------------------------------------------------------*/
	protected void setEnabledAllEditControls(boolean enabled)
	{
		setEnabledAllEditControls(editControls, enabled);
	}

	/*-------------------------------------------------------------------------*/
	protected static void setEnabledAllEditControls(Container parent, boolean enabled)
	{
		Component[] components = parent.getComponents();
		for (Component comp : components)
		{
			comp.setEnabled(enabled);
			if (comp instanceof Container)
			{
				setEnabledAllEditControls((Container)comp, enabled);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public void reload()
	{
		loadData();
		refreshNames(null);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Do not override this because that would break the EditorPanel behaviour
	 */
	public final void valueChanged(ListSelectionEvent e)
	{
		if (currentName != null)
		{
			commit(currentName);
		}

		currentName = (String)names.getSelectedValue();
		if (currentName == null)
		{
			return;
		}
		if (currentName != null)
		{
			refresh(currentName);
		}
	}

	/*-------------------------------------------------------------------------*/
	public int getDirtyFlag()
	{
		return dirtyFlag;
	}

	/*-------------------------------------------------------------------------*/
	protected abstract Container getEditControls();

	/*-------------------------------------------------------------------------*/
	/**
	 * @return
	 * 	The name of the currently selected item
	 */
	public String getCurrentName()
	{
		return (String)names.getSelectedValue();
	}

	/*-------------------------------------------------------------------------*/
	public void initForeignKeys()
	{
	}

	/*-------------------------------------------------------------------------*/
	public void keyPressed(KeyEvent e)
	{
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyTyped(KeyEvent e)
	{
		SwingUi.instance.setDirty(dirtyFlag);
	}

	/*-------------------------------------------------------------------------*/
	public void actionPerformed(ActionEvent e)
	{
		SwingUi.instance.setDirty(dirtyFlag);
	}

	/*-------------------------------------------------------------------------*/
	public void stateChanged(ChangeEvent e)
	{
		SwingUi.instance.setDirty(dirtyFlag);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void itemStateChanged(ItemEvent e)
	{
		SwingUi.instance.setDirty(dirtyFlag);
	}

	/*-------------------------------------------------------------------------*/
	protected boolean isDirty()
	{
		return SwingUi.instance.isDirty(dirtyFlag);
	}
	
	/*-------------------------------------------------------------------------*/
	protected static void dodgyGridBagShite(JPanel panel, Component a, Component b, GridBagConstraints gbc)
	{
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.gridx=0;
		gbc.gridy++;
		panel.add(a, gbc);
		gbc.weightx = 1.0;
		gbc.gridx++;
		panel.add(b, gbc);
	}

	/*-------------------------------------------------------------------------*/
	protected static GridBagConstraints createGridBagConstraints()
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2,2,2,2);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.FIRST_LINE_START;
		return gbc;
	}

	/*-------------------------------------------------------------------------*/
	public void tableChanged(TableModelEvent e)
	{
		SwingUi.instance.setDirty(dirtyFlag);
	}

	/*-------------------------------------------------------------------------*/
	public void mouseClicked(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	/*-------------------------------------------------------------------------*/
	public abstract void refresh(String name);

	public abstract void commit(String name);

	public abstract Collection<String> loadData();

	public abstract void newItem(String name);

	public abstract void renameItem(String newName);

	public abstract void copyItem(String newName);

	public abstract void deleteItem();

	public abstract boolean hasItem(String name);

	/*-------------------------------------------------------------------------*/

	/**
	 * Can be overridden by panels wanting their own creation UI.
	 */
	public void createNewItem()
	{
		String name = (String)JOptionPane.showInputDialog(
			SwingUi.instance,
			StringUtils.getUiString("ui.name"),
			StringUtils.getUiString("ui.new.item"),
			JOptionPane.QUESTION_MESSAGE,
			SwingUi.newIcon,
			null,
			null);

		if (name != null)
		{
			if (getCurrentName() != null)
			{
				commit(getCurrentName());
			}

			if (!checkAndConfirmDuplicateOverwrite(name))
			{
				return;
			}

			newItem(name);
			refreshNames(name);
			refresh(name);
			SwingUi.instance.setDirty(getDirtyFlag());
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	true if a duplicate item with the given name exists, but the user wants
	 * 	overwrite it anyway. False otherwise.
	 */
	public boolean checkAndConfirmDuplicateOverwrite(String name)
	{
		if (hasItem(name))
		{
			int i = JOptionPane.showConfirmDialog(
				SwingUi.instance,
				StringUtils.getUiString("ui.overwrite.msg", name),
				StringUtils.getUiString("ui.overwrite"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);

			return i != JOptionPane.NO_OPTION;
		}
		return true;
	}
}