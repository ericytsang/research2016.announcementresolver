package com.github.ericytsang.research2016.announcementresolver

import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import com.github.ericytsang.research2016.propositionallogic.BruteForceAnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.ComparatorBeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.OrderedAnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.toDnf
import com.sun.javafx.collections.ObservableListWrapper
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

// todo: extract magic strings
// todo: comment code
class AgentsWindow:Application()
{
    // GUI Strings

    /**
     * value for a [Menu] in the [menuBar].
     */
    private val FILE_MENU_TEXT = "File"

    /**
     * value for [MenuItem.text] of a menu item that saves entered input data to
     * a file.
     */
    private val SAVE_MENU_ITEM_TEXT = "Save"

    /**
     * value for [MenuItem.text] of a menu item that replaces input data on the
     * gui with data loaded from a file.
     */
    private val LOAD_MENU_ITEM_TEXT = "Load"

    // other constants

    /**
     * initial width of the window when the application starts.
     */
    private val WINDOW_WIDTH:Double = 700.0

    /**
     * initial height of the window when the application starts.
     */
    private val WINDOW_HEIGHT:Double = 400.0

    /**
     * spacing used between all GUI elements.
     */
    private val LAYOUT_SPACING:Double = Dimens.KEYLINE_SMALL.toDouble()

    /**
     * padding used between the edge of the window.
     */
    private val LAYOUT_PADDING:Insets = Insets(Dimens.KEYLINE_SMALL.toDouble())

    /**
     * lets the user specify the display format used for showing [Proposition]
     * objects.
     */
    private val displayModeComboBox = DisplayModeComboBox().apply()
    {
        // configure child nodes
        valueProperty().addListener(InvalidationListener()
        {
            agentsTableView.beliefStateToString = value.transform
        })
    }

    /**
     * enables user to input and view initial belief states, target belief
     * states and belief revision operators of agents. this table is also used
     * to display the revised belief state of agents if an announcement is
     * found.
     */
    private val agentsTableView = AgentsTableView(LAYOUT_SPACING)

    /**
     * used to display the announcement if one is found or an error message
     * otherwise.
     */
    private val announcementLabel = Label()

    /**
     * thread used to calculate announcement. initialized to a thread to get rid
     * of the need to check for null.
     */
    private var announcementFinderThread = thread {}

    /**
     * cancel button that allows user to abort of the announcement calculations.
     */
    private val cancelButton = Button().apply()
    {
        text = "Cancel"
        onAction = EventHandler()
        {
            announcementFinderThread.interrupt()
        }
        thread(isDaemon = true)
        {
            while (true)
            {
                Thread.sleep(500)
                val threadIsAlive = announcementFinderThread.isAlive
                val latch = CountDownLatch(1)
                Platform.runLater()
                {
                    isDisable = !threadIsAlive
                    latch.countDown()
                }
                latch.await()
            }
        }
    }

    /**
     * button used to begin finding the announcement that solves for the current
     * input.
     */
    private val findAnnouncementButton = Button().apply()
    {
        text = "Find announcement"
        onAction = EventHandler()
        {
            // display loading...
            announcementLabel.text = "Finding announcement..."
            isDisable = true

            announcementFinderThread = thread()
            {
                try
                {
                    // resolve the announcement
                    val problemInstances = agentsTableView.items.map {it.problemInstance}
                    val announcement = if (agentsTableView.items.any {it.problemInstance.beliefRevisionStrategy !is ComparatorBeliefRevisionStrategy})
                    {
                        BruteForceAnnouncementResolutionStrategy().resolve(problemInstances)
                    }
                    else
                    {
                        OrderedAnnouncementResolutionStrategy().resolve(problemInstances)
                    }

                    Platform.runLater()
                    {
                        // display the announcement
                        announcementLabel.text = announcement?.toDnf()?.toString()
                            ?: "No announcement found"
                    }

                    // display the revised belief states
                    if (announcement != null)
                    {
                        agentsTableView.items = agentsTableView.items
                            .map {it.copy(actualK = it.problemInstance.reviseBy(announcement))}
                            .let {ObservableListWrapper(it)}
                        Platform.runLater {agentsTableView.refresh()}
                    }
                }
                catch (ex:InterruptedException)
                {
                    Platform.runLater {announcementLabel.text = "Operation cancelled"}
                }
                finally
                {
                    Platform.runLater {isDisable = false}
                }
            }
        }
    }

    private val commitButton = Button().apply()
    {
        text = "Commit"
        onAction = EventHandler()
        {
            val newListItems = agentsTableView.items.map()
            {
                val newProblemInstance = it.problemInstance.copy(initialBeliefState = it.actualK)
                AgentsTableView.RowData(newProblemInstance,it.revisionFunctionConfigPanel,emptySet())
            }
            agentsTableView.items.clear()
            agentsTableView.items.addAll(newListItems)
            agentsTableView.refresh()
        }
    }

    private val menuBar = MenuBar().apply()
    {
        val fileMenu = Menu(FILE_MENU_TEXT).apply()
        {
            val saveMenuItem = MenuItem(SAVE_MENU_ITEM_TEXT).apply()
            {
                onAction = EventHandler {saveToFile()}
            }
            val loadMenuItem = MenuItem(LOAD_MENU_ITEM_TEXT).apply()
            {
                onAction = EventHandler {loadFromFile()}
            }
            items.addAll(saveMenuItem,loadMenuItem)
        }
        menus.addAll(fileMenu)
    }

    private val upperPane = VBox().apply()
    {
        VBox.setVgrow(agentsTableView,Priority.ALWAYS)
        agentsTableView.maxHeight = Double.MAX_VALUE

        spacing = LAYOUT_SPACING
        children.addAll(agentsTableView,displayModeComboBox)
    }

    private val lowerPane = HBox().apply()
    {
        HBox.setHgrow(announcementLabel,Priority.ALWAYS)
        announcementLabel.maxWidth = Double.MAX_VALUE

        spacing = LAYOUT_SPACING
        children.addAll(announcementLabel,cancelButton,findAnnouncementButton,commitButton)
    }

    private val rootPane = VBox().apply()
    {
        VBox.setVgrow(upperPane,Priority.ALWAYS)
        upperPane.maxHeight = Double.MAX_VALUE
        VBox.setVgrow(this,Priority.ALWAYS)
        maxHeight = Double.MAX_VALUE

        padding = LAYOUT_PADDING
        spacing = LAYOUT_SPACING
        children.addAll(upperPane,lowerPane)
    }

    private lateinit var primaryStage:Stage

    override fun start(primaryStage:Stage)
    {
        this.primaryStage = primaryStage
        primaryStage.title = "Announcement Finder"
        primaryStage.scene = Scene(VBox(menuBar,rootPane),WINDOW_WIDTH,WINDOW_HEIGHT)
        primaryStage.show()
    }

    private fun saveToFile()
    {
        val file = FileChooser()
            .apply()
            {
                title = "Save input data"
                initialDirectory = File(Paths.get(".").toAbsolutePath().normalize().toString())
            }
            .showSaveDialog(primaryStage)

        if (file != null)
        {
            val json = JSONArray()
            agentsTableView.items
                .map {it.problemInstance.toJSONObject()}
                .forEach {json.put(it)}

            PrintWriter(file.outputStream()).use()
            {
                it.print(json.toString(4))
            }
        }
    }

    private fun loadFromFile()
    {
        val file = FileChooser()
            .apply()
            {
                title = "Load input data"
                initialDirectory = File(Paths.get(".").toAbsolutePath().normalize().toString())
            }
            .showOpenDialog(primaryStage)

        if (file != null)
        {
            val fileText = file
                .inputStream()
                .use {it.readBytes().let {String(it)}}

            agentsTableView.items = fileText
                .let {JSONArray(it)}
                .map {it as JSONObject}
                .map {it.toProblemInstance()}
                .map {AgentsTableView.RowData(it,RevisionFunctionConfigPanel(),emptySet())}
                .apply {forEach {it.revisionFunctionConfigPanel.setValuesFrom(it.problemInstance.beliefRevisionStrategy)}}
                .let {ObservableListWrapper(it)}
        }
    }
}
