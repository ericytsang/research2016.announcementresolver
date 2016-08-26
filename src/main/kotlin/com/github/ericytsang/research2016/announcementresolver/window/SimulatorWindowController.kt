package com.github.ericytsang.research2016.announcementresolver.window

import com.github.ericytsang.lib.simulation.Looper
import com.github.ericytsang.lib.simulation.SimpleLooper
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.announcementresolver.simulation.Background
import com.github.ericytsang.research2016.announcementresolver.simulation.CanvasRenderer
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.net.URL
import java.util.ResourceBundle

// todo: add help message
class SimulatorWindowController:Initializable
{
    companion object
    {
        private const val WINDOW_TITLE = "Simulation"

        fun new():SimulatorWindowController
        {
            val loader = FXMLLoader(SimulatorWindowController::class.java.classLoader.getResource("simulatorwindow.fxml"))
            val root = loader.load<Parent>()
            return loader.getController<SimulatorWindowController>().apply()
            {
                stage = Stage()
                stage.scene = Scene(root)
                stage.title = WINDOW_TITLE
            }
        }
    }

    lateinit var stage:Stage
        private set

    @FXML lateinit var parentLayout:VBox

    @FXML lateinit var canvas:Canvas

    lateinit var simulation:Simulation

    // todo: allow user to right click on the smulator, and add stuff to the simulation via a context menu...

    override fun initialize(location:URL?,resources:ResourceBundle?)
    {
        simulation = Simulation(CanvasRenderer(canvas,32.0),Looper.Factory.new {SimpleLooper(it)})
        simulation.entityToCellsMap += Background() to emptySet()
    }

    /**
     * invoked by framework when the File > Close [MenuItem] is pressed.
     */
    @FXML private fun closeWindow()
    {
        stage.hide()
    }

    /**
     * invoked by framework when the Help > About [MenuItem] is pressed.
     */
    @FXML private fun showHelpDialog()
    {

    }
}
