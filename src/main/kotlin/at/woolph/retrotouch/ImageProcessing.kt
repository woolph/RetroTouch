package at.woolph.retrotouch

import java.util.concurrent.ThreadLocalRandom

import javafx.scene.image.PixelReader
import javafx.scene.image.PixelWriter
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color

import kotlin.math.*

fun Double.clip(min : Double, max : Double) = if (this<min) min else (if (this>max) max else this)
fun Double.clipToUni() = this.clip(0.0, 1.0)
fun Double.squared() = this*this
fun <T : Number> Number.to() : T { return this as T }

data class MyColor(val red : Double = 0.0, val green : Double = 0.0, val blue : Double = 0.0, val opacity : Double = 1.0) {
	fun toFXColor() = Color(red.clipToUni(), green.clipToUni(), blue.clipToUni(), opacity.clipToUni())

	fun invert() = MyColor(1-red, 1-green, 1-blue, opacity)

	operator fun plus(value : Double) = adjustBrightness(offset = value)
	operator fun minus(value : Double) = adjustBrightness(offset = -value)
	operator fun times(value : Double) = adjustBrightness(factor = value)
	operator fun div(value : Double) = adjustBrightness(factor = 1/value)

	operator fun unaryMinus() = MyColor(-red, -green, -blue, opacity)
	operator fun plus(value : MyColor) = MyColor(red+value.red, green+value.green, blue+value.blue, opacity)
	operator fun minus(value : MyColor) = MyColor(red-value.red, green-value.green, blue-value.blue, opacity)

	operator fun not() = invert()

	val length : Double
		get() = red*red + green*green + blue*blue

	fun adjustBrightness(offset : Double = 0.0, factor : Double = 1.0)
		= MyColor((red+offset)*factor, (green+offset)*factor, (blue+offset)*factor, opacity)

	fun adjustContrast(contrast : Double = 0.0) : MyColor {
		var factor = (1 + contrast * ContrastFactorA) / (1 - contrast * ContrastFactorB)

		var red = (factor*(this.red - 0.5) + 0.5)
		var green = (factor*(this.green - 0.5) + 0.5)
		var blue = (factor*(this.blue - 0.5) + 0.5)

		return MyColor(red, green, blue, opacity)
	}

	fun getQuantisedColor(palette : Iterable<MyColor>) : Pair<MyColor, MyColor> {
		var nearestDistance = Double.MAX_VALUE
		var nearestColor = this
		for(paletteColor in palette) {
			var distance = (this-paletteColor).length

			if(distance<nearestDistance) {
				nearestColor = paletteColor
				nearestDistance = distance
			}
		}
		return Pair(nearestColor, this-nearestColor)
	}

	companion object {
		fun random(factor : Double = 1.0) : MyColor {
			val randomizer = ThreadLocalRandom.current()
			return MyColor(factor*(2.0*randomizer.nextDouble()-1.0), factor*(2.0*randomizer.nextDouble()-1.0), factor*(2.0*randomizer.nextDouble()-1.0), 1.0)
		}

		val ColorChannelDepth = 8.0
		val SmallestColorDifference = 2.0.pow(-ColorChannelDepth)
		val ContrastFactorMax = 0.5/SmallestColorDifference
		val ContrastMax = 1.0
		val ContrastFactorA = 1.0/ContrastMax
		val ContrastFactorB = 1.0/(ContrastMax/(1-1/ContrastFactorMax*(1+ContrastMax/ContrastFactorA)))
	}
}

fun Color.toMyColor() = MyColor(this.red, this.green, this.blue, this.opacity)

fun intermediateColors(c1 : Color, c2 : Color, steps : Int) : Collection<Color> {
	val list = ArrayList<Color>()
	val factor = 1.0/(steps-1)
	for(i in 0..steps-1) {
		list.add(c1.interpolate(c2, factor*i))
	}
	return list
}

fun intermediateColors(c1 : MyColor, c2 : MyColor, steps : Int) : Collection<MyColor> {
	return intermediateColors(c1.toFXColor(), c2.toFXColor(), steps).map(Color::toMyColor)
}

operator fun PixelReader.get(x : Int, y : Int) = this.getColor(x, y).toMyColor()
operator fun PixelReader.get(pos : IntPoint) = this[pos.x, pos.y]
operator fun PixelWriter.set(x : Int, y : Int, color : MyColor) : MyColor  {
	this.setColor(x, y, color.toFXColor())
	return color
}
operator fun PixelWriter.set(pos : IntPoint, color : MyColor) = this.set(pos.x, pos.y, color)

operator fun Image.get(x : Int, y : Int) = this.getPixelReader().getColor(x, y).toMyColor()
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
			var xFactor = when(xt) {
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
					color += this[xt, yt]*xyFactor
					colorFactor += xyFactor
				}
			}
		}
	}
	return color/colorFactor
}
operator fun WritableImage.set(x : Int, y : Int, color : MyColor) { this.getPixelWriter().setColor(x, y, color.toFXColor()) }
operator fun WritableImage.set(x : Int, y : Int, pixelSize : IntPoint, color : MyColor) {
	for(i in x*pixelSize.x until (x+1)*pixelSize.x) {
		if(i<this.sizeX) {
			for(j in y*pixelSize.y until (y+1)*pixelSize.y) {
				if(j<this.sizeY) {
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
fun Image.process() : WritableImage {
	val pixelRatioX = 4
	val pixelRatioY = 3
	var targetPixelSize = calcPixelSize(pixelRatioX, pixelRatioY)

	val (scaleX, scaleY) = getScaleFactors(0.33, targetPixelSize)
	val targetWidth = this.width*scaleX // TODO choosable scaling down with factor or fixed resolution (take pixel size into consideration to remain aspect ratio)
	val targetHeight = this.height*scaleY // TODO choosable scaling down with factor or fixed resolution (take pixel size into consideration to remain aspect ratio)

	val sourceWindowSize = DoublePoint(this.width/targetWidth, this.height/targetHeight)

	val result = WritableImage(targetWidth.toInt()*targetPixelSize.x, targetHeight.toInt()*targetPixelSize.y)


	val palette = arrayOf(Color.BLACK, Color.WHITE, Color.RED, Color.LIME, Color.BLUE, Color.GREY, Color.GREEN, Color.YELLOW).map(Color::toMyColor)
	//val palette = intermediateColors(Color.WHITE, Color.GREEN, 8).union(intermediateColors(Color.BLACK, Color.GREEN, 8)).map { x -> x.toMyColor() }
	val errorDiffusion = ErrorDiffusionMap(result.sizeX, result.sizeY)
	val center = result.size.toDoublePoint()/2.0

	val brightness = 0.2

	//val vignettingFilter = VignettingFilter(center, center.lengthSquared*0.5, 0.1, 0.5)
	val vignettingFilter = VignettingFilterEllipse(center, result.size.toDoublePoint()*2.0, -5.0, 0.2)

	for(x in result.rangeX) {
		for(y in result.rangeY) {
			val pos = DoublePoint(x, y)
			var color = this[x, y, sourceWindowSize]

			//color = Color.WHITE.toMyColor()

			// invert
			//color = color.invert()

			// brightness
			//color = color + brightness

			// vignettierung
			//color = color - vignettingFilter[x, y]

			// contrast
			//color = color.adjustContrast(0.25)

			// TODO pixel displacement

			// noise
			//color = color + MyColor.random(0.2*cos(y.toDouble()/this.height*PI)*cos(x.toDouble()/this.width*PI))

			// color reduction to palette
			var (quantisedColor, quantisationError) = (color + errorDiffusion[x, y]).getQuantisedColor(palette)

			// Floyd-Steinberg-Dithering
			errorDiffusion.applyErrorDiffusionKernel(x, y, quantisationError*0.8, ErrorDiffusionKernel.MINIMIZED_AVERAGE_ERROR)
			//color = quantisedColor

			result[x, y, targetPixelSize] = color
		}
	}

	return result
}
