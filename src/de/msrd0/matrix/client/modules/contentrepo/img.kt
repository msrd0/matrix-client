/*
 matrix-client
 Copyright (C) 2018 Dominic Meiser
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0>.
*/

package de.msrd0.matrix.client.modules.contentrepo

import java.awt.image.RenderedImage

data class ImageInfo(
		val width : Int,
		val height : Int,
		val mimetype : String,
		val size : Int
)
{
	constructor(image : RenderedImage, mimetype : String, size : Int)
			: this(image.width, image.height, mimetype, size)
}
