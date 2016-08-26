package com.github.ericytsang.research2016.announcementresolver.window

import com.github.ericytsang.lib.javafxutils.JavafxUtils
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.announcementresolver.guicomponent.ObstacleTableView
import com.github.ericytsang.research2016.announcementresolver.persist.ObstaclesSaveFileParser
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.nio.file.Paths

/**
 * Created by surpl on 8/14/2016.
 */
// todo: save and load
// todo: add help message
class ObstacleWindowController
{
    companion object
    {
        private const val WINDOW_TITLE = "Obstacles"

        fun new():ObstacleWindowController
        {
            val loader = FXMLLoader(ObstacleWindowController::class.java.classLoader.getResource("obstaclewindow.fxml"))
            val root = loader.load<Parent>()
            return loader.getController<ObstacleWindowController>().apply()
            {
                stage = Stage()
                stage.scene = Scene(root)
                stage.title = WINDOW_TITLE
            }
        }
    }

    lateinit var stage:Stage
        private set

    @FXML lateinit var obstacleTableView:ObstacleTableView

    /**
     * invoked by framework when the File > Save [MenuItem] is pressed.
     */
    @FXML private fun saveToFile()
    {
        val file = FileChooser()
            .apply()
            {
                title = "Save Input Data"
                initialDirectory = File(Paths.get(".").toAbsolutePath().normalize().toString())
                extensionFilters += FileChooser.ExtensionFilter("obstacles","*.obstacles")
                extensionFilters += FileChooser.ExtensionFilter("json","*.json")
                extensionFilters += FileChooser.ExtensionFilter("any","*")
            }
            .showSaveDialog(stage.scene.window)

        if (file != null)
        {
            try
            {
                val objects = obstacleTableView.items
                    .map {ObstaclesSaveFileParser.Obstacle(it.cell1.x,it.cell1.y,it.cell2.x,it.cell2.y)}
                ObstaclesSaveFileParser.save(file,objects)
            }
            catch (ex:Exception)
            {
                JavafxUtils.showErrorDialog("Save Input Data","Failed to save input data",ex)
            }
        }
    }

    /**
     * invoked by framework when the File > Load [MenuItem] is pressed.
     */
    @FXML private fun loadFromFile()
    {
        // let the user pick a file to load from
        val file = FileChooser()
            .apply()
            {
                title = "Load Input Data"
                initialDirectory = File(Paths.get(".").toAbsolutePath().normalize().toString())
                extensionFilters += FileChooser.ExtensionFilter("obstacles","*.obstacles")
                extensionFilters += FileChooser.ExtensionFilter("json","*.json")
                extensionFilters += FileChooser.ExtensionFilter("any","*")
            }
            .showOpenDialog(stage.scene.window)

        // if a file was chosen, load data from it
        if (file != null)
        {
            try
            {
                val parsedObjects = ObstaclesSaveFileParser.load(file)

                // load agents from the file
                val objects = parsedObjects
                    .map {ObstacleTableView.RowData(
                        Simulation.Cell.getElseMake(it.position1X,it.position1Y),
                        Simulation.Cell.getElseMake(it.position2X,it.position2Y))}

                // add them to the table view
                obstacleTableView.items.setAll(objects)
            }
            catch (ex:Exception)
            {
                JavafxUtils.showErrorDialog("Load Input Data","Failed to load input data",ex)
            }
        }
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
