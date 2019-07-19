package at.woolph.retrotouch

import javafx.scene.paint.Color
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class MyColor(val red : Double = 0.0, val green : Double = 0.0, val blue : Double = 0.0, val opacity : Double = 1.0) {
	fun toFXColor() = Color(red.clipToUni(), green.clipToUni(), blue.clipToUni(), opacity.clipToUni())
	fun clipToUni() = MyColor(red.clipToUni(), green.clipToUni(), blue.clipToUni(), opacity.clipToUni())
	fun invert() = MyColor(1-red, 1-green, 1-blue, opacity)

	operator fun plus(value : Double) = adjustBrightness(offset = value)
	operator fun minus(value : Double) = adjustBrightness(offset = -value)
	operator fun times(value : Number) = adjustBrightness(factor = value.toDouble())
	operator fun div(value : Number) = adjustBrightness(factor = 1.0/value.toDouble())

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

	/**
	 * see https://www.pocketmagic.net/enhance-saturation-in-images-programatically/
	 */
	fun getHSB(): Triple<Double, Double, Double> {
		if(red==green && green==blue) {
			return Triple<Double, Double, Double>(0.0, 0.0, red)
		} else {
			val maxColor = max(red, max(green, blue))
			val minColor = min(red, min(green, blue))
			val distance = maxColor - minColor
			val brightness = (maxColor+minColor)/2
			val saturation = if (brightness<0.5) distance/(maxColor+minColor) else distance/(2.0 - distance)
			val hue = (when(maxColor) {
				red -> (green - blue) / distance
				green -> 2.0 + (blue - red) / distance
				else -> 4.0 + (red - blue) / distance
			} * 60.0) % 360.0
			return Triple<Double, Double, Double>(hue, saturation, brightness)
		}
	}

	fun adjustSaturation(saturation : Double = 0.0) : MyColor {
		val (h, s, b) = this.getHSB()
		return MyColor.getFromHSB(h, (s+saturation).clipToUni(), b, opacity)
	}

	fun adjustHue(hue : Double = 0.0) : MyColor {
		val (h, s, b) = this.getHSB()
		return MyColor.getFromHSB(h+hue, s, b, opacity)
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

		/**
		 * see https://www.pocketmagic.net/enhance-saturation-in-images-programatically/
		 */
		fun getFromHSB(hue : Double, saturation : Double, brightness : Double, opacity : Double = 1.0) : MyColor {
			if(saturation==0.0) {
				return MyColor(brightness, brightness, brightness, opacity)
			} else {
				val hue2 = (hue % 360.0) / 360.0
				val temp2 = if(brightness<0.5) brightness * (1 + saturation) else (brightness + saturation) - (brightness * saturation)
				val temp1 = 2 * brightness - temp2

				var tempr = hue2 + 1.0 / 3.0
				if(tempr > 1.0) tempr -= 1.0
				var tempg = hue2
				var tempb = hue2 - 1.0 / 3.0
				if(tempb < 0.0) tempb += 1.0

				val r = when {
					tempr < 1.0 / 6.0 -> temp1 + (temp2 - temp1) * 6.0 * tempr
					tempr < 0.5       -> temp2
					tempr < 2.0 / 3.0 -> temp1 + (temp2 - temp1) * ((2.0 / 3.0) - tempr) * 6.0
					else              -> temp1
				}

				val g = when {
					tempg < 1.0 / 6.0 -> temp1 + (temp2 - temp1) * 6.0 * tempg
					tempg < 0.5       -> temp2
					tempg < 2.0 / 3.0 -> temp1 + (temp2 - temp1) * ((2.0 / 3.0) - tempg) * 6.0
					else              -> temp1
				}

				val b = when {
					tempb < 1.0 / 6.0 -> temp1 + (temp2 - temp1) * 6.0 * tempb
					tempb < 0.5       -> temp2
					tempb < 2.0 / 3.0 -> temp1 + (temp2 - temp1) * ((2.0 / 3.0) - tempb) * 6.0
					else              -> temp1
				}
				return MyColor(r, g, b, opacity)
			}
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
	val factor = 1.0/(steps.toDouble()-1.0)
	for(i in 0..steps-1) {
		list.add(c1.interpolate(c2, factor*i))
	}
	return list
}

fun intermediateColors(c1 : MyColor, c2 : MyColor, steps : Int) : Collection<MyColor> {
	return intermediateColors(c1.toFXColor(), c2.toFXColor(), steps).map(Color::toMyColor)
}
