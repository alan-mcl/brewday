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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.document;

import freemarker.template.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;

/**
 * Uses freemarker to create a document from a template
 */
public class DocumentCreator
{
	public static final String FREEMARKER_EXTENSION = ".fth";
	private static final DocumentCreator instance;

	static
	{
		try
		{
			instance = new DocumentCreator();
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}
	}

	/** Freemarker configuration, cached per the lib recommendation. */
	private final Configuration cfg;

	/*-------------------------------------------------------------------------*/
	public DocumentCreator() throws IOException
	{
		// set up freemarker config
		cfg = new Configuration(Configuration.VERSION_2_3_29);
		cfg.setDirectoryForTemplateLoading(Database.getInstance().getTemplateDir());
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
	}

	public static DocumentCreator getInstance()
	{
		return instance;
	}

	/*-------------------------------------------------------------------------*/
	public void createDocument(Recipe recipe, String templateName, File outputFile) throws IOException
	{
		Template template = cfg.getTemplate(templateName);
		Writer out = new FileWriter(outputFile);
		Properties docLabels = Database.getInstance().getStrings("document");

		Map<String, Object> ftlRoot = new HashMap<>();
		ftlRoot.put("recipe", recipe);
		ftlRoot.put("beers", recipe.getBeers());
		ftlRoot.put("version", Brewday.getInstance().getAppConfig().getProperty("mclachlan.brewday.version"));
		ftlRoot.put("labels", docLabels);

		try
		{
			template.process(ftlRoot, out);

			out.flush();
			out.close();
		}
		catch (TemplateException e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		Database db = Database.getInstance();
		db.loadAll();

		DocumentCreator dc = DocumentCreator.getInstance();

		Recipe r = db.getRecipes().get("Test Recipe 1");
		r.run();

		dc.createDocument(r, "brew_steps.txt.ftl", new File("./test.txt"));
	}
}
