/*
 * matrix-client
 * Copyright (C) 2017 Julius Lehmann
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

package org.matrix.olm;

import java.io.*;
import java.util.logging.Logger;

import cz.adamh.utils.NativeUtils;

/**
 * Olm SDK entry point class.<br> An OlmManager instance must be created at first to enable native library load.
 * <br><br>Detailed implementation guide is available at <a href="http://matrix.org/docs/guides/e2e_implementation.html">Implementing End-to-End Encryption in Matrix clients</a>.
 */
public class OlmManager
{
	private static final Logger LOGGER = Logger.getLogger(OlmManager.class.getName());
	
	/**
	 * Constructor.
	 */
	public OlmManager()
	{
		try
		{
			NativeUtils.loadLibraryFromJar("/libolm.so");
			NativeUtils.loadLibraryFromJar("/libolmjava.so");
		}
		catch (IOException e)
		{
			LOGGER.severe("Exception loadLibrary() - Msg=" + e.getMessage());
		}
	}
	
	/**
	 * Provide the native OLM lib version.
	 *
	 * @return the lib version as a string
	 */
	public String getOlmLibVersion()
	{
		return getOlmLibVersionJni();
	}
	
	private native String getOlmLibVersionJni();
}

