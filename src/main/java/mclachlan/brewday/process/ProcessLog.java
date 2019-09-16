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

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.StringUtils;

/**
 *
 */
public class ProcessLog
{
	private List<String> msgs, errors, warnings;

	public ProcessLog()
	{
		msgs = new ArrayList<>();
		errors = new ArrayList<>();
		warnings = new ArrayList<>();
	}

	public void addMessage(String s) {msgs.add(s); }

	public void addError(String s)
	{
		msgs.add(StringUtils.getProcessString("log.error", s));
		errors.add(s);
	}

	public void addWarning(String s)
	{
		msgs.add(StringUtils.getProcessString("log.warning", s));
		warnings.add(s);
	}

	public List<String> getMsgs()
	{
		return msgs;
	}

	public List<String> getErrors()
	{
		return errors;
	}

	public List<String> getWarnings()
	{
		return warnings;
	}

	public void clear()
	{
		this.msgs.clear();
		this.errors.clear();
		this.warnings.clear();
	}
}
