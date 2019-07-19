package at.woolph.retrotouch

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.layout.BorderPane
import javafx.geometry.Rectangle2D
import javafx.stage.FileChooser
import tornadofx.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.system.exitProcess

class MyApp : App(MyView::class) {
}

enum class DisplayMode {
	FULL_HORIZONTAL, FULL_VERTICAL, SPLIT_HORIZONTAL, SPLIT_VERTICAL
}

// add gui
// TODO implement palette editor
class MyView : View() {
	override val root = BorderPane()
	val counter = SimpleIntegerProperty()
	val brightness = SimpleIntegerProperty()
	val contrast = SimpleIntegerProperty()

	val originalImage: Image
	val modifiedImageProperty = SimpleObjectProperty<WritableImage>(null)
	var modifiedImage by modifiedImageProperty

	val displayType = DisplayMode.FULL_HORIZONTAL

	init {
		title = "RetroTouch"

		originalImage = chooseFile("Open Image", arrayOf(FileChooser.ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg"))).singleOrNull()?.let { Image(it.toURI().toString()) } ?: exitProcess(0)

		modifiedImage = originalImage.process()

		//val palette = generateColorPalette(originalImage, 8)

		//openInternalWindow(PaletteEditor::class)

		with(root) {
			style {
				padding = box(20.px)
			}
			top {
				toolbar {
					button("Save Result") {
						action {
							chooseFile("Save Image", arrayOf(FileChooser.ExtensionFilter("PNG", "*.png")), mode = FileChooserMode.Save).singleOrNull()?.let {
								ImageIO.write(SwingFXUtils.fromFXImage(modifiedImage, null), "png", it)
							}
						}
					}

					togglebutton("Effect") {
						selectedProperty().addListener { _, _, newValue ->

							modifiedImage = originalImage.process(newValue)
						}
					}

				}
			}

			center {
				vbox(10.0) {
					alignment = Pos.CENTER

					when (displayType) {
						DisplayMode.FULL_HORIZONTAL -> hbox(0.0) {
							imageview(originalImage) {
								fitHeight = 800.0
								fitWidth = 800.0
								setPreserveRatio(true)
							}
							imageview {
								imageProperty().bind(modifiedImageProperty)
								fitHeight = 800.0
								fitWidth = 800.0
								setPreserveRatio(true)
							}
						}
						DisplayMode.FULL_VERTICAL -> vbox(0.0) {
							imageview(originalImage)
							imageview() {
								imageProperty().bind(modifiedImageProperty)
								fitHeight = 800.0
								fitWidth = 800.0
								setPreserveRatio(true)
							}
						}
						DisplayMode.SPLIT_HORIZONTAL -> hbox(0.0) {
							imageview(originalImage) {
								viewport = Rectangle2D(0.0, 0.0, originalImage.width * 0.5, originalImage.height)
								fitHeight = 800.0
								fitWidth = 800.0
								setPreserveRatio(true)
							}
							imageview(modifiedImage) {
								viewport = Rectangle2D(modifiedImage.width * 0.5, 0.0, modifiedImage.width, modifiedImage.height)
								fitHeight = 800.0
								fitWidth = 800.0
								setPreserveRatio(true)
							}
						}
						DisplayMode.SPLIT_VERTICAL -> vbox(0.0) {
							imageview(originalImage) {
								viewport = Rectangle2D(0.0, 0.0, originalImage.width, originalImage.height * 0.5)
								fitHeight = 800.0
								fitWidth = 800.0
								setPreserveRatio(true)
							}
							imageview(modifiedImage) {
								viewport = Rectangle2D(0.0, modifiedImage.height * 0.5, modifiedImage.width, modifiedImage.height)
								fitHeight = 800.0
								fitWidth = 800.0
								setPreserveRatio(true)
							}
						}
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
			bottom {

			}
		}
	}
}
