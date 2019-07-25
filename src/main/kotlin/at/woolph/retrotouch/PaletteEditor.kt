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
import tornadofx.rectangle
import tornadofx.imageview
import tornadofx.label
import tornadofx.px
import tornadofx.slider
import tornadofx.style
import tornadofx.vbox
import java.awt.image.BufferedImage

class PaletteEditor(palette : MutableSet<MyColor>) : View() {
	override val root = BorderPane()

	init {
		title = "RetroTouch - Palette Editor"

		with(root) {
				style {
						padding = box(20.px)
				}

				center {
						vbox(10.0) {
							for(color in palette) {
								rectangle(0.0, 0.0, 25.0, 25.0) {
									setArcWidth(5.0)
									setArcHeight(5.0)
									setFill(color.toFXColor())
								}
							}
						}
				}
		}
	}
}
