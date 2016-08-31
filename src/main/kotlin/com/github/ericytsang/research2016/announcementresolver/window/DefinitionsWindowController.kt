package com.github.ericytsang.research2016.announcementresolver.window

import com.github.ericytsang.lib.javafxutils.JavafxUtils
import com.github.ericytsang.research2016.announcementresolver.guicomponent.DefinitionsTableView
import com.github.ericytsang.research2016.announcementresolver.persist.DefinitionsSaveFileParser
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
// todo: add help message
class DefinitionsWindowController
{
    companion object
    {
        private const val WINDOW_TITLE = "Definitions"

        fun new():DefinitionsWindowController
        {
            val loader = FXMLLoader(DefinitionsWindowController::class.java.classLoader.getResource("definitionswindow.fxml"))
            val root = loader.load<Parent>()
            return loader.getController<DefinitionsWindowController>().apply()
            {
                stage = Stage()
                stage.scene = Scene(root)
                stage.title = WINDOW_TITLE
            }
        }
    }

    lateinit var stage:Stage
        private set

    @FXML lateinit var definitionsTableView:DefinitionsTableView

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
                extensionFilters += FileChooser.ExtensionFilter("definitions","*.definitions")
                extensionFilters += FileChooser.ExtensionFilter("json","*.json")
                extensionFilters += FileChooser.ExtensionFilter("any","*")
            }
            .showSaveDialog(stage.scene.window)

        if (file != null)
        {
            try
            {
                val objects = definitionsTableView.items
                    .map {DefinitionsSaveFileParser.BehaviourEntry(it.proposition,it.behavior)}
                DefinitionsSaveFileParser.save(file,objects)
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
                extensionFilters += FileChooser.ExtensionFilter("definitions","*.definitions")
                extensionFilters += FileChooser.ExtensionFilter("json","*.json")
                extensionFilters += FileChooser.ExtensionFilter("any","*")
            }
            .showOpenDialog(stage.scene.window)

        // if a file was chosen, load data from it
        if (file != null)
        {
            try
            {
                val parsedObjects = DefinitionsSaveFileParser.load(file)

                // load agents from the file
                val objects = parsedObjects
                    .map {DefinitionsTableView.RowData(it.proposition,it.behaviour)}

                // add them to the table view
                definitionsTableView.items.setAll(objects)
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
