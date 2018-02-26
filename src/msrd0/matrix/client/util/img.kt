/*
 * matrix-client
 * Copyright (C) 2017-2018 Dominic Meiser
 * Copyright (C) 2017 Julius Lehmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0>.
 */

@file:JvmName("ImageUtil")
package msrd0.matrix.client.util

import java.awt.Image
import java.awt.image.*
import java.util.*

fun RenderedImage.toBufferedImage() : BufferedImage
{
	if (this is BufferedImage)
		return this
	
	val raster = colorModel.createCompatibleWritableRaster(width, height)
	val props = Hashtable<String, Any>()
	for (key in (propertyNames ?: emptyArray()))
		props[key] = getProperty(key)
	
	val img = BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, props)
	img.copyData(raster)
	return img
}

fun RenderedImage.toImage() : Image
{
	if (this is Image)
		return this
	return toBufferedImage()
}
