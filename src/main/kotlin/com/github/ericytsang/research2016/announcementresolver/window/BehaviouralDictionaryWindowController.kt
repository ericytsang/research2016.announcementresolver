package com.github.ericytsang.research2016.announcementresolver.window

import com.github.ericytsang.research2016.announcementresolver.guicomponent.BehavioralDictionaryTableView
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

/**
 * Created by surpl on 8/14/2016.
 */
// todo: save and load
// todo: add help message
class BehaviouralDictionaryWindowController
{
    companion object
    {
        private const val WINDOW_TITLE = "Behavioural Dictionary"

        fun new():BehaviouralDictionaryWindowController
        {
            val loader = FXMLLoader(BehaviouralDictionaryWindowController::class.java.classLoader.getResource("behavioraldictionarywindow.fxml"))
            val root = loader.load<Parent>()
            return loader.getController<BehaviouralDictionaryWindowController>().apply()
            {
                stage = Stage()
                stage.scene = Scene(root)
                stage.title = WINDOW_TITLE
            }
        }
    }

    lateinit var stage:Stage
        private set

    @FXML lateinit var behaviouralDictionaryTableView:BehavioralDictionaryTableView

    /**
     * invoked by framework when the File > Save [MenuItem] is pressed.
     */
    @FXML private fun saveToFile()
    {
    }

    /**
     * invoked by framework when the File > Load [MenuItem] is pressed.
     */
    @FXML private fun loadFromFile()
    {
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
