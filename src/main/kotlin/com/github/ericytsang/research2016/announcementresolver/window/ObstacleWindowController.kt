package com.github.ericytsang.research2016.announcementresolver.window

import com.github.ericytsang.research2016.announcementresolver.guicomponent.ObstacleTableView
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

/**
 * Created by surpl on 8/14/2016.
 */
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
}
