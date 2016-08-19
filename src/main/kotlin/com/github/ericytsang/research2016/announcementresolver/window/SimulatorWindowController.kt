package com.github.ericytsang.research2016.announcementresolver.window

import com.github.ericytsang.lib.simulation.Looper
import com.github.ericytsang.lib.simulation.SimpleLooper
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.announcementresolver.simulation.Background
import com.github.ericytsang.research2016.announcementresolver.simulation.CanvasRenderer
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.layout.VBox
import java.net.URL
import java.util.ResourceBundle

class SimulatorWindowController:Initializable
{
    @FXML lateinit var parentLayout:VBox

    @FXML lateinit var canvas:Canvas

    lateinit var simulation:Simulation<CanvasRenderer.Renderee>

    // todo: allow user to right click on the smulator, and add stuff to the simulation via a context menu...

    override fun initialize(location:URL?,resources:ResourceBundle?)
    {
        simulation = Simulation(CanvasRenderer(canvas,32.0),Looper.Factory.new {SimpleLooper(it)})
        simulation.entityToCellsMap += Background() to emptySet()
    }
}
