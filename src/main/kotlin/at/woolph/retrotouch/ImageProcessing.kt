package at.woolph.retrotouch

import javafx.scene.image.PixelReader
import javafx.scene.image.PixelWriter
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color

import kotlin.math.*

operator fun PixelReader.get(x : Int, y : Int) = this.getColor(x, y).toMyColor()
operator fun PixelReader.get(pos : IntPoint) = this[pos.x, pos.y]
operator fun PixelWriter.set(x : Int, y : Int, color : MyColor) : MyColor  {
	this.setColor(x, y, color.toFXColor())
	return color
}
operator fun PixelWriter.set(pos : IntPoint, color : MyColor) = this.set(pos.x, pos.y, color)

//operator fun Image.get(x : Int, y : Int) = this.getPixelReader().getColor(x, y).toMyColor()
operator fun Image.get(x : Int, y : Int): MyColor? {
	return if(0 <= x && x < this.sizeX && 0 <= y && y < this.sizeY)
		this.pixelReader.getColor(x, y).toMyColor()
	else
		null
}

operator fun Image.get(x : Int, y : Int, sourceWindowSize : DoublePoint) : MyColor {
	val xStart = floor(x*sourceWindowSize.x).toInt()
	val xStartFactor = 1.0 - x*sourceWindowSize.x + xStart
	val xEnd = ceil((x+1)*sourceWindowSize.x).toInt() - 1
	val xEndFactor = (x+1)*sourceWindowSize.x - xEnd
	val yStart = floor(y*sourceWindowSize.y).toInt()
	val yStartFactor = 1.0 - y*sourceWindowSize.y + yStart
	val yEnd = ceil((y+1)*sourceWindowSize.y).toInt() - 1
	val yEndFactor = (y+1)*sourceWindowSize.y - yEnd
//	val xSize = xEnd-xStart
//	val ySize = yEnd-yStart

	var color = MyColor()
	var colorFactor = 0.0
	for(xt in xStart..xEnd) {
		if(xt<this.sizeX) {
			val xFactor = when(xt) {
				xStart -> xStartFactor
				xEnd -> xEndFactor
				else -> 1.0
			}
			for(yt in yStart..yEnd) {
				if(yt<this.sizeY) {
					var xyFactor = xFactor * when(yt) {
						yStart -> yStartFactor
						yEnd -> yEndFactor
						else -> 1.0
					}
					this[xt, yt]?.let {
						color += it*xyFactor
						colorFactor += xyFactor
					}
				}
			}
		}
	}
	return color/colorFactor
}

operator fun WritableImage.set(x : Int, y : Int, color : MyColor) { this.getPixelWriter().setColor(x, y, color.toFXColor()) }
operator fun WritableImage.set(x : Int, y : Int, pixelSize : IntPoint, color : MyColor) {
	for(i in x*pixelSize.x until (x+1)*pixelSize.x) {
		if(0<=i && i<this.sizeX) {
			for(j in y*pixelSize.y until (y+1)*pixelSize.y) {
				if(0<=j && j<this.sizeY) {
					this.set(i, j, color)
				}
			}
		}
	}
}

val Image.sizeX : Int
	get() = this.width.toInt()

val Image.sizeY : Int
	get() = this.height.toInt()

val Image.rangeX : IntRange
	get() = 0 until this.sizeX

val Image.rangeY : IntRange
	get() = 0 until this.sizeY

val Image.size : IntPoint
	get() = IntPoint(this.width.toInt(), this.height.toInt())

class ErrorDiffusionMap(val sizeX : Int, val sizeY : Int){
	val errorDiffusion = Array<Array<MyColor>>(this.sizeX, { x -> Array(this.sizeY, { y -> MyColor() }) })

	operator fun get(x : Int, y : Int) : MyColor {
		if(x>=0 && x<sizeX && y>=0 && y<sizeY) {
			return errorDiffusion[x][y]
		}
		return MyColor()
	}

	operator fun set(x : Int, y : Int, color : MyColor) {
		if(x>=0 && x<sizeX && y>=0 && y<sizeY) {
			errorDiffusion[x][y] = color
		}
	}

	fun applyErrorDiffusionKernel(x : Int, y : Int, quantisationError : MyColor, kernel : Iterable<ErrorDiffusionKernel.KernelElement>) {
		for(kernelElement in kernel) {
			this[x + kernelElement.deltaX, y + kernelElement.deltaY] += quantisationError * kernelElement.factor
		}
	}
}



class ErrorDiffusionKernel {
	data class KernelElement(val deltaX : Int, val deltaY : Int, val factor : Double)

	companion object {
		// https://en.wikipedia.org/wiki/Error_diffusion
		val NO_DITHERING = ArrayList<KernelElement>()

		val SIMPLE_DITHERING = listOf(
				KernelElement( 1, 0, 0.5),
				KernelElement( 0, 1, 0.5)
			)

		val FLOYD_STEINBERG = listOf(
				KernelElement( 1, 0, 7.0 / 16.0),
				KernelElement(-1, 1, 3.0 / 16.0),
				KernelElement( 0, 1, 5.0 / 16.0),
				KernelElement( 1, 1, 1.0 / 16.0)
			)

		val MINIMIZED_AVERAGE_ERROR = listOf(
				KernelElement( 1, 0, 7.0 / 48.0),
				KernelElement( 2, 0, 5.0 / 48.0),
				KernelElement(-2, 1, 3.0 / 48.0),
				KernelElement(-1, 1, 5.0 / 48.0),
				KernelElement( 0, 1, 7.0 / 48.0),
				KernelElement(+1, 1, 5.0 / 48.0),
				KernelElement(+2, 1, 3.0 / 48.0),
				KernelElement(-2, 2, 1.0 / 48.0),
				KernelElement(-1, 2, 3.0 / 48.0),
				KernelElement( 0, 2, 5.0 / 48.0),
				KernelElement(+1, 2, 3.0 / 48.0),
				KernelElement(+2, 2, 1.0 / 48.0)
			)

		val CUSTOM_DITHERING = listOf(
				KernelElement( 1, 0, 5.0 / 48.0),
				KernelElement( 2, 0, 7.0 / 48.0),
				KernelElement(-2, 1, 3.0 / 48.0),
				KernelElement(-1, 1, 7.0 / 48.0),
				KernelElement( 0, 1, 5.0 / 48.0),
				KernelElement(+1, 1, 7.0 / 48.0),
				KernelElement(+2, 1, 3.0 / 48.0),
				KernelElement(-2, 2, 1.0 / 48.0),
				KernelElement(-1, 2, 3.0 / 48.0),
				KernelElement( 0, 2, 7.0 / 48.0),
				KernelElement(+1, 2, 3.0 / 48.0),
				KernelElement(+2, 2, 1.0 / 48.0)
			)
	}
}
// TODO generic Point for Numbers in general?! integer as well
data class DoublePoint(val x : Double, val y : Double) {
	constructor(x : Int, y : Int) : this(x.toDouble(), y.toDouble())

	val lengthSquared : Double
		get() = x*x + y*y
	val length : Double
		get() = sqrt(lengthSquared)

	operator fun plus(p : DoublePoint) = DoublePoint(x+p.x, y+p.y)
	operator fun minus(p : DoublePoint) = DoublePoint(x-p.x, y-p.y)
	operator fun times(value : Number) = DoublePoint(x*value.toDouble(), y*value.toDouble())
	operator fun div(value : Number) = DoublePoint(x/value.toDouble(), y/value.toDouble())

	operator fun unaryMinus() = DoublePoint(-x, -y)

	fun toIntPoint() = IntPoint(x.toInt(), y.toInt())
}

data class IntPoint(val x : Int, val y : Int) {
	val lengthSquared : Int
		get() = x*x + y*y
	val length : Double
		get() = sqrt(lengthSquared.toDouble())

	operator fun plus(p : IntPoint) = IntPoint(x+p.x, y+p.y)
	operator fun minus(p : IntPoint) = IntPoint(x-p.x, y-p.y)
	operator fun times(value : Number) = IntPoint(x*value.toInt(), y*value.toInt())
	operator fun div(value : Number) = IntPoint(x/value.toInt(), y/value.toInt())

	operator fun unaryMinus() = IntPoint(-x, -y)

	fun toDoublePoint() = DoublePoint(x.toDouble(), y.toDouble())
}

data class VignettingFilter(val center : DoublePoint, val length : Double, val rollOff : Double, val strength : Double = 1.0) {
	val rollOffFactor = strength/(exp(rollOff)-1)

	operator fun get(x : Int, y : Int) : Double {
		val pos = DoublePoint(x.toDouble(), y.toDouble())
		val factor = (pos-center).lengthSquared/length
		return if(factor<1.0) (exp(factor*rollOff)-1)*rollOffFactor else strength
	}
}

data class VignettingFilterEllipse(val center : DoublePoint, val size : DoublePoint, val rollOff : Double, val strength : Double = 1.0) {
	val rollOffFactor = strength/(exp(rollOff)-1)
	val cd : Double
	val d : Double
	val focalPoint1 : DoublePoint
	val focalPoint2 : DoublePoint

	init {
		var lanscape = size.x>size.y
		val c = if (lanscape) size.x else size.y
		d = if (lanscape) sqrt(size.x*size.x - size.y*size.y) else sqrt(size.y*size.y - size.x*size.x)
		var p = if (lanscape) DoublePoint(d*0.5, 0.0) else DoublePoint(0.0, d*0.5)
		cd = c-d
		focalPoint1 = center + p
		focalPoint2 = center - p
	}

	operator fun get(pos : DoublePoint) : Double {
		val factor = (((pos-focalPoint1).length+(pos-focalPoint2).length-d)/cd).squared()
		return if(factor<1.0) (exp(factor*rollOff)-1)*rollOffFactor else 1.0
	}
	operator fun get(x : Int, y : Int) = this[DoublePoint(x.toDouble(), y.toDouble())]
}

fun greatestCommonDivisor(i1 : Int, i2 : Int) : Int {
	var n1 = if (i1 > 0) i1 else -i1
	var n2 = if (i2 > 0) i2 else -i2

	while (n1 != n2) {
		if (n1 > n2) {
			n1 -= n2
		} else {
			n2 -= n1
		}
	}
	return n1
}

fun calcPixelSize(n1 : Int, n2 : Int) : IntPoint {
	val gcd = greatestCommonDivisor(n1, n2)
	return IntPoint(n1/gcd, n2/gcd)
}

fun calcPixelSize(n1 : Double, n2 : Double, precision : Double) : IntPoint {
	return calcPixelSize((n1*precision).toInt(), (n2*precision).toInt())
}
fun getScaleFactors(scale : Double, pixelSize : IntPoint) : Pair<Double, Double> {
	var factor = 1/max(pixelSize.x.toDouble(), pixelSize.y.toDouble())
	return Pair<Double, Double>(scale*pixelSize.y*factor, scale*pixelSize.x*factor)
}

fun getDoubleRange(start : Double, end : Double, steps : Int) : Collection<Double> {
	val span = end-start
	return (0 .. steps-1).map({ step : Int -> start+span*step.toDouble()/(steps-1) } as (Int) -> Double)
}

fun getDoubleRange2(start : Double, end : Double, steps : Int) : Collection<Double> {
	val span = end-start
	return (0 until steps).map({ step : Int -> start+span*step.toDouble()/(steps) } as (Int) -> Double)
}

fun generateColorPalette(hueSteps : Int, saturationSteps : Int, brightnessSteps : Int, hueOffset : Double = 0.0) : MutableSet<MyColor> {
	return generateColorPalette(
		getDoubleRange2(hueOffset, 360.0+hueOffset, hueSteps),
		getDoubleRange(0.0, 1.0, saturationSteps),
		getDoubleRange(0.0, 1.0, brightnessSteps))
}


fun generateColorPalette(hues : Collection<Double>, saturations : Collection<Double>, brightnesses : Collection<Double>) : MutableSet<MyColor> {
	val palette = mutableSetOf<MyColor>()

	for(hue in hues) {
		val h = hue % 360.0
		for(saturation in saturations) {
			val s = saturation.clipToUni()
			for(brightness in brightnesses) {
				palette.add(Color.hsb(h, s, brightness.clipToUni()).toMyColor())
			}
		}
	}

	return palette
}

fun generateColorPalette(image : Image, threshold : Double) : MutableSet<MyColor> {
	val palette = mutableMapOf<MyColor, Int>()

	// TODO unfortunately no direct control over palette size (color count) ?!

	for(x in image.rangeX) {
		for(y in image.rangeY) {
			var color = image[x, y]!!

			var nearestDistance = Double.MAX_VALUE
			var nearestColor = color

			for(paletteColor in palette.keys) {
				var distance = (color-paletteColor).length

				if(distance <= threshold && distance < nearestDistance) {
					nearestColor = paletteColor
					nearestDistance = distance
				}
			}

			if(nearestDistance <= threshold) {
				var weight = palette[nearestColor] ?: 1
				var newColor = (nearestColor * weight + color) / (1.0 + weight)
				palette.remove(nearestColor) // TODO not very efficient to cosntantly remove and add items, maybe better to work with index an fixed array?!
				palette[newColor] = weight + 1
			} else {
				palette[color] = 1
			}
		}
	}
	return palette.keys
}

fun generateColorPalette(image : Image, colorCount : Int) : MutableSet<MyColor> {
	val palette = Array<MyColor>(colorCount, { i -> MyColor() })
	val weights = Array<Int>(colorCount, { i -> 0 })

	var threshold = 0.0

	for(x in image.rangeX) {
		for(y in image.rangeY) {
			var color = image[x, y]!!

			var nearestDistance = Double.MAX_VALUE
			var nearestColorIndex = -1

			loop@ for(i in palette.indices) {
				if(weights[i] == 0 ) {
					nearestColorIndex = i
					nearestDistance = -1.0
					break@loop
				} else {
					var distance = (color-palette[i]).length

					if(distance < nearestDistance) {
						nearestColorIndex = i
						nearestDistance = distance
					}
				}
			}

			if(nearestColorIndex >= 0) {
				if(nearestDistance <= threshold) { // if color distance is beneath threshold, combine them
					var weight = weights[nearestColorIndex]
					weights[nearestColorIndex] = weight + 1
					palette[nearestColorIndex] = (palette[nearestColorIndex] * weight + color) / weights[nearestColorIndex]
				} else { // otherwise check if we should combine existing color to make place for the new one
					var closestPairIndex1 = 0
					var closestPairIndex2 = 1
					var closestPairDistance = Double.MAX_VALUE
					// TODO consider new color for combination as well
					// TODO closestDistance is now the new threshold!

					for(i1 in 0 until colorCount-1) {
						for(i2 in i1+1 until colorCount) {
							var distance = (palette[i1]-palette[i2]).length

							if(distance < closestPairDistance) {
								closestPairIndex1 = i1
								closestPairIndex2 = i2
								closestPairDistance = distance
							}
						}
					}

					// if no pairing of existing palette is closer, than the new color to one of the palette color
					if(nearestDistance < closestPairDistance) {
						// combine the new color with the nearestPalette color and make their distance the new threshold
						var weight = weights[nearestColorIndex]
						weights[nearestColorIndex] = weight + 1
						palette[nearestColorIndex] = (palette[nearestColorIndex] * weight + color) / weights[nearestColorIndex]
						threshold = nearestDistance
					} else { // otherwise (if a pairing of existing palette is closer)
						// combine this palette colors to free an entry in the palette for the new color
						var weight1 = weights[closestPairIndex1]
						var weight2 = weights[closestPairIndex2]

						weights[closestPairIndex1] = weight1 + weight2
						palette[closestPairIndex1] = (palette[closestPairIndex1] * weight1 + palette[closestPairIndex2] * weight2) / weights[closestPairIndex1]

						threshold = closestPairDistance // make their distance the new threshold

						// and of course add the new color to the freed entry
						weights[closestPairIndex2] = 1
						palette[closestPairIndex2] = color
					}
				}
			}
		}
	}
	return palette.toMutableSet()
}

fun WritableImage.bloom(threshold: Double, weight: (Int, Int)->Double) {

}

fun Image.process(effect: Boolean = true, scale: Double = 1.0, brightness: Double = 0.0, contrast: Double = 0.0) : WritableImage {
	val pixelRatioX = 1
	val pixelRatioY = 1
	var targetPixelSize = calcPixelSize(pixelRatioX, pixelRatioY)

	val (scaleX, scaleY) = getScaleFactors(scale, targetPixelSize)
	val targetWidth = floor(this.width*scaleX) // TODO choosable scaling down with factor or fixed resolution (take pixel size into consideration to remain aspect ratio)
	val targetHeight = floor(this.height*scaleY) // TODO choosable scaling down with factor or fixed resolution (take pixel size into consideration to remain aspect ratio)

	val sourceWindowSize = DoublePoint(this.width/targetWidth, this.height/targetHeight)

	val result = WritableImage(targetWidth.toInt()*targetPixelSize.x, targetHeight.toInt()*targetPixelSize.y)

	//val palette = generateColorPalette(hueSteps=6, saturationSteps=5, brightnessSteps=5, hueOffset=0.0)
	val palette = generateColorPalette(this, 32)
	//val palette = mutableListOf(Color.BLACK, Color.WHITE, Color.RED, Color.LIME, Color.BLUE, Color.GREY, Color.GREEN, Color.YELLOW).map(Color::toMyColor)
	println(palette.size)
	println(palette.map(MyColor::toFXColor).map { c -> c.toString().substring(2,8) })

	val errorDiffusion = ErrorDiffusionMap(result.sizeX, result.sizeY)
	val center = result.size.toDoublePoint()/2.0

	//val vignettingFilter = VignettingFilter(center, center.lengthSquared*0.5, 0.1, 0.5)
	val vignettingFilter = VignettingFilterEllipse(center, result.size.toDoublePoint()*2.0, -5.0, 0.2)


	// bloom
	val bloomRangeX = -5 .. 5
	val bloomRangeY = -5 .. 5
	val bloomThreshold = 0.7
	val calcBloomWeight: (Int, Int)->Double = { dx, dy -> if(dx != 0 || dy != 0) 1.0/(dx*dx+dy*dy).toDouble() else 0.0 }
	val bloomThresholdFactor = 1/(1-bloomThreshold) // precalculated constant

	for(x in result.rangeX) {
		for(y in result.rangeY) {
			val pos = DoublePoint(x, y)
			var color = this[x, y, sourceWindowSize]

			//color = Color.WHITE.toMyColor()

			// invert
			//color = color.invert()

			// brightness
			color = color.adjustBrightness(brightness)

			// bloom
			/*if(effect) {
				var bloomSum = MyColor()
				var bloomWeightSum = 0.0
				for(dx in bloomRangeX) {
					for(dy in bloomRangeY) {
						this[x + dx, y + dy, sourceWindowSize]?.let {
							val bloomWeight = calcBloomWeight(dx, dy)
							val bloom = bloomWeight * ((it.getHSB().third - bloomThreshold) * bloomThresholdFactor).clipToUni()
							bloomSum += it * bloom
							bloomWeightSum += bloomWeight
						}
					}
				}
				color += bloomSum/bloomWeightSum
			}*/

			// vignettierung
			//color = color - vignettingFilter[x, y]

			// contrast
			//color = color.adjustContrast(contrast)

			// saturation
			//color = color.adjustSaturation(1.0)

			// noise
			//color = color + MyColor.random(0.05*cos(y.toDouble()/this.height*PI)*cos(x.toDouble()/this.width*PI))

			// eliminate greenish colors for 80s puprlish coloring
			// by setting green channel to zero and rising red and blue channel
			// accordingly to maintain perceived brightnessSteps
			/* color = color.clipToUni()
			val red2 = color.red.squared() * 0.241 // perceived brigthness factor according to http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
			val green2 = color.green.squared() * 0.691
			val blue2 = color.blue.squared() * 0.068
			val factor = sqrt((red2+green2+blue2)/(red2+blue2))
			color = MyColor(factor * color.red, 0.0, factor * color.blue, 1.0) */

			// color reduction to palette
			var (quantisedColor, quantisationError) = (color + errorDiffusion[x, y]).clipToUni().getQuantisedColor(palette)

			// Floyd-Steinberg-Dithering
			errorDiffusion.applyErrorDiffusionKernel(x, y, quantisationError*0.67, ErrorDiffusionKernel.MINIMIZED_AVERAGE_ERROR)
			color = quantisedColor


			// pixel displacement
			var x2 = x //+ round(0.5+0.5*sin(y.toDouble()*0.09)*cos(y.toDouble()*0.7)).toInt()
			var y2 = y // + round(5.0*sin(x.toDouble()*0.09)*cos(x.toDouble()*0.7)).toInt()

			result[x2, y2, targetPixelSize] = color

			// filling up empty pixels due to pixel displacement
			if(x!=x2 && (x==0 || x==result.sizeX-1)) {
				val range = if(x==0) 0 until x2 else x2+1 until result.sizeX
				for(x3 in range) {
					result[x3, y2, targetPixelSize] = color
				}
			}
			if(y!=y2 && (y==0 || y==result.sizeY-1)) {
				val range = if(y==0) 0 until y2 else y2+1 until result.sizeY
				for(y3 in range) {
					result[x2, y3, targetPixelSize] = color
				}
			}
		}
	}



	val result2 = WritableImage(result.sizeX, result.sizeY)

	for(x in result.rangeX) {
		for(y in result.rangeY) {
			var color2 = result[x, y]!!

			// bloom
			if(effect) {
				var bloomSum = MyColor()
				var bloomWeightSum = 0.0
				for(dx in bloomRangeX) {
					for(dy in bloomRangeY) {
						result[x + dx, y + dy]?.let {
							val bloomWeight = calcBloomWeight(dx, dy)
							val bloom = bloomWeight * ((it.getHSB().third - bloomThreshold) * bloomThresholdFactor).clipToUni()
							if(bloom > 0.0) {
								bloomSum += it * bloom
								bloomWeightSum += bloomWeight
							}
						}
					}
				}
				color2 += bloomSum/bloomWeightSum

				result2[x, y] = color2
			}
		}
	}

	return result2
}
