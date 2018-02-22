package at.woolph.retrotouch

import java.util.concurrent.ThreadLocalRandom

import javafx.scene.image.PixelReader
import javafx.scene.image.PixelWriter
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color

fun Double.clip(min : Double, max : Double) = if (this<min) min else (if (this>max) max else this)

fun Double.clipToUni() = this.clip(0.0, 1.0)

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

	fun adjustContrast(offset : Double = 0.0, factor : Double = 1.0) : MyColor {
		return MyColor() // TODO
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
operator fun PixelWriter.set(x : Int, y : Int, color : MyColor) { this.setColor(x, y, color.toFXColor()) }

operator fun Image.get(x : Int, y : Int) = this.getPixelReader().getColor(x, y).toMyColor()
operator fun WritableImage.set(x : Int, y : Int, color : MyColor) { this.getPixelWriter().setColor(x, y, color.toFXColor()) }

val Image.sizeX : Int
	get() = this.width.toInt()

val Image.sizeY : Int
	get() = this.height.toInt()

val Image.rangeX : IntRange
	get() = 0 until this.sizeX

val Image.rangeY : IntRange
	get() = 0 until this.sizeY

val Image.size : Point
	get() = Point(this.width, this.height)

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
data class Point(val x : Double, val y : Double) {
	val lengthSquared : Double
		get() = x*x + y*y
	val length : Double
		get() = kotlin.math.sqrt(lengthSquared)

	operator fun plus(p : Point) = Point(x+p.x, y+p.y)
	operator fun minus(p : Point) = Point(x-p.x, y-p.y)
	operator fun times(value : Double) = Point(x*value, y*value)
	operator fun div(value : Double) = times(1/value)

	operator fun unaryMinus() = Point(-x, -y)
}

data class VignettingFilter(val center : Point, val length : Double, val strength : Double, val rollOff : Double) {
	val rollOffFactor = strength/(kotlin.math.exp(rollOff)-1)

	operator fun get(x : Int, y : Int) : Double {
		val pos = Point(x.toDouble(), y.toDouble())
		return (kotlin.math.exp((pos-center).lengthSquared/length*rollOff)-1)*rollOffFactor
	}
}

fun WritableImage.set(originalImage : Image) {
	//val pixelWriter = this.getPixelWriter()
	//val pixelReader = image.getPixelReader()
	val palette = arrayOf(Color.BLACK, Color.WHITE, Color.RED, Color.LIME, Color.BLUE).map(Color::toMyColor)
	//val palette = intermediateColors(Color.WHITE, Color.GREEN, 8).union(intermediateColors(Color.BLACK, Color.GREEN, 8)).map { x -> x.toMyColor() }
	val errorDiffusion = ErrorDiffusionMap(this.sizeX, this.sizeY)
	val center = this.size/2.0

	val brightness = 0.2

	val vignettingFilter = VignettingFilter(center, center.lengthSquared*0.75, 0.5, 0.1)

	for(x in this.rangeX) {
		for(y in this.rangeY) {
			val pos = Point(x.toDouble(), y.toDouble())
			var color = originalImage[x, y]
			//color = color.deriveColor(0.0, 1.0, 1.0, 1.0)
			//color = color.invert()

			color = color + brightness

			// vignettierung
			color = color - vignettingFilter[x, y]

			// TODO adjustContrast
			// TODO scale down
			// TODO pixelRatio
			// TODO pixel displacement

			// noise
			//color = color + MyColor.random(0.1*kotlin.math.cos(y.toDouble()/this.height*kotlin.math.PI)*kotlin.math.cos(x.toDouble()/this.width*kotlin.math.PI))

			// color reduction to palette
			var (quantisedColor, quantisationError) = (color + errorDiffusion[x, y]).getQuantisedColor(palette)

			// Floyd-Steinberg-Dithering
			errorDiffusion.applyErrorDiffusionKernel(x, y, quantisationError, ErrorDiffusionKernel.MINIMIZED_AVERAGE_ERROR)

			this[x, y] = quantisedColor
		}
	}
}