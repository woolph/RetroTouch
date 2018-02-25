package at.woolph.retrotouch

import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.layout.BorderPane
import javafx.geometry.Rectangle2D
import tornadofx.App
import tornadofx.View
import tornadofx.bind
import tornadofx.box
import tornadofx.button
import tornadofx.center
import tornadofx.hbox
import tornadofx.imageview
import tornadofx.label
import tornadofx.px
import tornadofx.slider
import tornadofx.style
import tornadofx.vbox
import java.awt.image.BufferedImage

class MyApp : App() {
		override val primaryView = MyView::class
}

enum class DisplayMode {
		FULL_HORIZONTAL, FULL_VERTICAL, SPLIT_HORIZONTAL, SPLIT_VERTICAL
}

class MyView : View() {
	override val root = BorderPane()
	val counter = SimpleIntegerProperty()
	val brightness = SimpleIntegerProperty()
	val contrast = SimpleIntegerProperty()

	val originalImage = Image("file:/E:/2016-08-22 17.31.10.jpg", 800.0, 800.0, true, true)

	val displayType = DisplayMode.FULL_HORIZONTAL

	init {
		title = "test"

		val modifiedImage = originalImage.process()

		with(root) {
				style {
						padding = box(20.px)
				}

				center {
						vbox(10.0) {
								alignment = Pos.CENTER

								when (displayType) {
										DisplayMode.FULL_HORIZONTAL -> hbox(0.0) {
													imageview(originalImage)
													imageview(modifiedImage) {
														setPreserveRatio(true)
														setFitHeight(800.0)
														setFitWidth(800.0)
													}
												}
										DisplayMode.FULL_VERTICAL -> vbox(0.0) {
													imageview(originalImage)
													imageview(modifiedImage) {
														setPreserveRatio(true)
														setFitHeight(800.0)
														setFitWidth(800.0)
													}
												}
										DisplayMode.SPLIT_HORIZONTAL -> hbox(0.0) {
													imageview(originalImage) {
														viewport = Rectangle2D(0.0, 0.0, originalImage.width*0.5, originalImage.height)
													}
													imageview(modifiedImage) {
														viewport = Rectangle2D(originalImage.width*0.5, 0.0, originalImage.width, originalImage.height)
													}
												}
										DisplayMode.SPLIT_VERTICAL -> vbox(0.0) {
													imageview(originalImage) {
														viewport = Rectangle2D(0.0, 0.0, originalImage.width, originalImage.height*0.5)
													}
													imageview(modifiedImage) {
														viewport = Rectangle2D(0.0, originalImage.height*0.5, originalImage.width, originalImage.height)
													}
												}
										else -> {}
								}
								/*hbox(10.0) {
				slider(-255,255,0){
					bind(brightness)
				}
									label() {
											bind(brightness)
											style { fontSize = 25.px }
									}
			}
								hbox(10.0) {
				slider(-255,255,0){
					bind(contrast)
				}
									label() {
											bind(contrast)
											style { fontSize = 25.px }
									}
			}
								hbox(10.0) {
									label() {
											bind(counter)
											style { fontSize = 25.px }
									}
									button("Click to increment").setOnAction {
											increment()
									}
			}*/
						}
				}
		}
		}

		fun increment() {
				counter.value += 1
		}
}
