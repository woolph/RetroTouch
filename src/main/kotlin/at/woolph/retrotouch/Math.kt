package at.woolph.retrotouch

/**
 * clips value to [min, max]
 * @return min if value is smaller than min, max if value is greater than max otherwise the value itself
 */
fun Double.clip(min : Double, max : Double) = if (this<min) min else (if (this>max) max else this)

/**
 * clips value to [0.0, 1.0]
 * @return zero if value is smaller than zero, one if value is greater than one otherwise the value itself
 */
fun Double.clipToUni() = this.clip(0.0, 1.0)
fun Double.squared() = this*this
//fun <T : Number> Number.to() : T { return this as T }


infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
	require(start.isFinite())
	require(endInclusive.isFinite())
	require(step > 0.0) { "Step must be positive, was: $step." }
	val sequence = generateSequence(start) { previous ->
		if (previous == Double.POSITIVE_INFINITY) return@generateSequence null
		val next = previous + step
		if (next > endInclusive) null else next
	}
	return sequence.asIterable()
}
