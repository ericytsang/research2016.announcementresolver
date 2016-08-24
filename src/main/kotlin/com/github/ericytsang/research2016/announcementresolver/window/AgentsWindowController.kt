package com.github.ericytsang.research2016.announcementresolver.window

import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.announcementresolver.guicomponent.AgentsTableView
import com.github.ericytsang.research2016.announcementresolver.guicomponent.DisplayModeComboBox
import com.github.ericytsang.research2016.announcementresolver.guicomponent.RevisionFunctionConfigPanel
import com.github.ericytsang.research2016.announcementresolver.simulation.AgentController
import com.github.ericytsang.research2016.announcementresolver.simulation.Behaviour
import com.github.ericytsang.research2016.announcementresolver.simulation.CanvasRenderer
import com.github.ericytsang.research2016.announcementresolver.simulation.Obstacle
import com.github.ericytsang.research2016.announcementresolver.simulation.VirtualAgentController
import com.github.ericytsang.research2016.announcementresolver.simulation.Wall
import com.github.ericytsang.research2016.announcementresolver.toJSONObject
import com.github.ericytsang.research2016.announcementresolver.toProblemInstance
import com.github.ericytsang.research2016.propositionallogic.BruteForceAnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.ComparatorBeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.OrderedAnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.toDnf
import com.sun.javafx.collections.ObservableListWrapper
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Point2D
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.Label
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.net.URL
import java.nio.file.Paths
import java.util.LinkedHashMap
import java.util.ResourceBundle
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

// todo: add help message
class AgentsWindowController:Initializable
{
    companion object
    {
        private const val WINDOW_TITLE = "Announcement Finder"

        fun new():AgentsWindowController
        {
            // load the main content pane and add it to a window and show it
            val loader = FXMLLoader(AgentsWindowController::class.java.classLoader.getResource("agentswindow.fxml"))
            val root = loader.load<Parent>()
            return loader.getController<AgentsWindowController>().apply()
            {
                stage = Stage()
                stage.title = WINDOW_TITLE
                stage.scene = Scene(root)

                // when primaryStage window is closed, close all peripheral windows too.
                stage.scene.window.onHidden = EventHandler()
                {
                    hideAllPeripheralWindows()
                }
            }
        }
    }

    /**
     * reference to the parent stage.
     */
    lateinit var stage:Stage
        private set

    /**
     * field is initialized by the JavaFx framework.
     */
    @FXML private lateinit var rootLayout:Parent

    /**
     * field is initialized by the JavaFx framework.
     *
     * [CheckMenuItem] that when clicked, calls [toggleDictionaryWindow].
     */
    @FXML private lateinit var toggleDictionaryWindowCheckMenuItem:CheckMenuItem

    /**
     * field is initialized by the JavaFx framework.
     *
     * [CheckMenuItem] that when clicked, calls [toggleObstacleWindow].
     */
    @FXML private lateinit var toggleObstacleWindowCheckMenuItem:CheckMenuItem

    /**
     * field is initialized by the JavaFx framework.
     *
     * [CheckMenuItem] that when clicked, calls [toggleSimulationWindow].
     */
    @FXML private lateinit var toggleSimulationWindowCheckMenuItem:CheckMenuItem

    /**
     * field is initialized by the JavaFx framework.
     *
     * enables user to input and view initial belief states, target belief
     * states and belief revision operators of agents. this table is also used
     * to display the revised belief state of agents if an announcement is
     * found.
     */
    @FXML private lateinit var agentsTableView:AgentsTableView

    /**
     * field is initialized by the JavaFx framework.
     *
     * lets the user specify the display format used for showing [Proposition]
     * objects.
     */
    @FXML private lateinit var displayModeComboBox:DisplayModeComboBox

    /**
     * field is initialized by the JavaFx framework.
     *
     * used to display the announcement if one is found or an error message
     * otherwise.
     */
    @FXML private lateinit var announcementLabel:Label

    /**
     * field is initialized by the JavaFx framework.
     *
     * cancel button that allows user to abort of the announcement calculations.
     */
    @FXML private lateinit var cancelButton:Button

    /**
     * field is initialized by the JavaFx framework.
     *
     * button used to begin finding the announcement that solves for the current
     * input.
     */
    @FXML private lateinit var findAnnouncementButton:Button

    /**
     * thread used to calculate announcement. initialized to a thread to get rid
     * of the need to check for null.
     */
    private var announcementFinderThread = thread {}

    /**
     * window used by user to map variables to robot behaviours.
     */
    private val behaviouralDictionaryWindowController = BehaviouralDictionaryWindowController.new()

    private val obstacleWindowController = ObstacleWindowController.new()

    private val simulationWindowController = SimulatorWindowController.new()

    override fun initialize(location:URL?,resources:ResourceBundle?)
    {
        /*
         * thread that enables/disables [cancelButton] based on status of
         * [announcementFinderThread].
         */
        thread(isDaemon = true)
        {
            while (true)
            {
                Thread.sleep(500)
                val threadIsAlive = announcementFinderThread.isAlive
                val latch = CountDownLatch(1)
                Platform.runLater()
                {
                    cancelButton.isDisable = !threadIsAlive
                    latch.countDown()
                }
                latch.await()
            }
        }

        /*
         * when the behaviouralDictionaryWindowController's tableview items are changed,
         * update the dictionaries in the agents as well
         */
        behaviouralDictionaryWindowController.behaviouralDictionaryTableView.items.addListener(InvalidationListener()
        {
            val oldItems = agentsTableView.items.toList()
            agentsTableView.items.setAll(oldItems)
        })

        /*
         * when the obstacleWindowController's tableview items are changed,
         * update the obstacles in the agents as well
         */
        obstacleWindowController.obstacleTableView.items.addListener(InvalidationListener()
        {
            val oldItems = agentsTableView.items.toList()
            agentsTableView.items.setAll(oldItems)
        })

        /*
         * [toggleDictionaryWindowCheckMenuItem] displays a check mark beside
         * itself when [behaviouralDictionaryWindow] is showing; no check mark is displayed
         * otherwise.
         */
        behaviouralDictionaryWindowController.stage.scene.window.onShown = EventHandler()
        {
            toggleDictionaryWindowCheckMenuItem.isSelected = true
        }
        behaviouralDictionaryWindowController.stage.scene.window.onHidden = EventHandler()
        {
            toggleDictionaryWindowCheckMenuItem.isSelected = false
        }

        /*
         * [toggleObstacleWindowCheckMenuItem] displays a check mark beside
         * itself when [obstacleWindow] is showing; no check mark is displayed
         * otherwise.
         */
        obstacleWindowController.stage.scene.window.onShown = EventHandler()
        {
            toggleObstacleWindowCheckMenuItem.isSelected = true
        }
        obstacleWindowController.stage.scene.window.onHidden = EventHandler()
        {
            toggleObstacleWindowCheckMenuItem.isSelected = false
        }

        /*
         * [toggleSimulationWindowCheckMenuItem] displays a check mark beside
         * itself when [simulationWindow] is showing; no check mark is displayed
         * otherwise.
         */
        simulationWindowController.stage.scene.window.onShown = EventHandler()
        {
            toggleSimulationWindowCheckMenuItem.isSelected = true
        }
        simulationWindowController.stage.scene.window.onHidden = EventHandler()
        {
            toggleSimulationWindowCheckMenuItem.isSelected = false
        }

        /*
         * when walls are added to the [obstacleWindow], they are also added to
         * the [simulationWindow]'s canvas
         */
        obstacleWindowController.obstacleTableView.items.addListener(InvalidationListener()
        {
            val keysToRemove = simulationWindowController.simulation.entityToCellsMap.keys.filter {it is Wall}
            keysToRemove.forEach()
            {
                simulationWindowController.simulation.entityToCellsMap.remove(it)
            }

            simulationWindowController.simulation.entityToCellsMap
                .putAll(obstacleWindowController.obstacleTableView.items.associate {Wall(it.cell1,it.cell2) to setOf(it.cell1,it.cell2)})
        })

        /*
         * when the agent table view is modified, turn them into
         * [AgentController] objects, and add them to the simulation.
         */
        agentsTableView.items.addListener(InvalidationListener()
        {
            val existingAgents = simulationWindowController.simulation.entityToCellsMap.keys
                .filter {it is AgentController}
                .map {it as AgentController}
                .associate {it.agentId to it}
                .let {LinkedHashMap(it)}

            // resolve agents removed from the table view and remove them from the simulation
            val remainingAgentIds = agentsTableView.items
                .map {it.agentId}
            val keysToRemove = existingAgents
                .filter {it.value.agentId !in remainingAgentIds}
                .map {it.value}
            keysToRemove.forEach()
            {
                simulationWindowController.simulation.entityToCellsMap.remove(it)
            }

            // resolve new agents added to the table view and add them to the simulation
            val newRows = agentsTableView.items.filter {it.agentId !in existingAgents.keys}

            // todo: make this instantiate virtual or actual robot connections...
            // todo: make connecting to agents asynchronsous because...it won't be an instantaneous thing when connecting to real robots
            val newAgentControllers = newRows.map()
            {
                rowData ->
                val agentController = VirtualAgentController(rowData.agentId)
                existingAgents[agentController.agentId] = agentController
                agentController.connect()
                agentController
            }
            simulationWindowController.simulation.entityToCellsMap
                .putAll(newAgentControllers.associate {it to emptySet<Simulation.Cell>()})

            // resolve updated agents in the table update them in the simulation
            val updatedRows = agentsTableView.items.filter {it.agentId in existingAgents.keys}
            updatedRows.forEach()
            {
                rowData ->
                val agentController = existingAgents[rowData.agentId]!!
                // todo: instead of setting their belief state, should be revising instead!!
                agentController.setBeliefState(rowData.problemInstance.initialBeliefState)
                agentController.setBeliefRevisionStrategy(rowData.problemInstance.beliefRevisionStrategy)
                agentController.setBehaviourDictionary(behaviouralDictionaryWindowController.behaviouralDictionaryTableView.items.map {it.proposition to it.behavior})
                agentController.setObstacles(simulationWindowController.simulation.entityToCellsMap.filter {it.key is Obstacle}.flatMap {it.value}.toSet())
                agentController.bodyColor = rowData.color

                // setting the position and direction of the robot to the user-specified one if it exists
                if (rowData.shouldJumpToInitialPosition)
                {
                    agentController.position = rowData.newPosition.let {Point2D(it.x.toDouble(),it.y.toDouble())}
                    agentController.direction = rowData.newDirection.angle
                }
            }

            // automatically deactivite the "jump to initial position flag"
            val newListItems = agentsTableView.items
                .map {it.copy(shouldJumpToInitialPosition = false)}
            if (newListItems != agentsTableView.items)
            {
                agentsTableView.items.setAll(newListItems)
            }
        })
    }

    fun hideAllPeripheralWindows()
    {
        behaviouralDictionaryWindowController.stage.hide()
        obstacleWindowController.stage.hide()
        simulationWindowController.stage.hide()
    }

    /**
     * invoked by framework when value of [displayModeComboBox] changes.
     */
    @FXML fun onDisplayModeChanged()
    {
        agentsTableView.beliefStateToString = displayModeComboBox.value.transform
    }

    /**
     * field is initialized by the JavaFx framework.
     *
     * invoked by framework when [cancelButton] is clicked. stops announcement
     * finding calculations.
     */
    @FXML private fun cancelAnnouncementFinding()
    {
        announcementFinderThread.interrupt()
    }

    /**
     * invoked by framework when [findAnnouncementButton] is clicked.
     */
    @FXML private fun beginAnnouncementFinding()
    {
        // display loading...
        announcementLabel.text = "Finding announcement..."
        findAnnouncementButton.isDisable = true

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
                    val newListItems = agentsTableView.items
                        .map {it.copy(actualK = it.problemInstance.reviseBy(announcement))}

                    for (i in newListItems.indices)
                    {
                        agentsTableView.items[i] = newListItems[i]
                    }

                    Platform.runLater {agentsTableView.refresh()}
                }
            }
            catch (ex:InterruptedException)
            {
                Platform.runLater {announcementLabel.text = "Operation cancelled"}
            }
            finally
            {
                Platform.runLater {findAnnouncementButton.isDisable = false}
            }
        }
    }

    /**
     * invoked by framework when Commit button pressed.
     */
    @FXML private fun commitAnnouncement()
    {
        if (agentsTableView.items.all {it.actualK.isNotEmpty()})
        {
            val newListItems = agentsTableView.items.map()
            {
                existingRowData ->
                val newProblemInstance = existingRowData.problemInstance.copy(initialBeliefState = existingRowData.actualK)
                existingRowData.copy(problemInstance = newProblemInstance,actualK = emptySet())
            }

            for (i in newListItems.indices)
            {
                agentsTableView.items[i] = newListItems[i]
            }

            agentsTableView.refresh()
        }
    }

    /**
     * invoked by framework when the Window > Behavioural Dictionary
     * [CheckMenuItem] pressed.
     */
    @FXML private fun toggleDictionaryWindow()
    {
        if (behaviouralDictionaryWindowController.stage.isShowing)
        {
            behaviouralDictionaryWindowController.stage.hide()
        }
        else
        {
            behaviouralDictionaryWindowController.stage.show()
        }
    }

    /**
     * invoked by framework when the Window > Obstacles [CheckMenuItem] pressed.
     */
    @FXML private fun toggleObstacleWindow()
    {
        if (obstacleWindowController.stage.isShowing)
        {
            obstacleWindowController.stage.hide()
        }
        else
        {
            obstacleWindowController.stage.show()
        }
    }

    /**
     * invoked by framework when the Window > Simulation [CheckMenuItem]
     * pressed.
     */
    @FXML private fun toggleSimulationWindow()
    {
        if (simulationWindowController.stage.isShowing)
        {
            simulationWindowController.stage.hide()
        }
        else
        {
            simulationWindowController.stage.show()
        }
    }
    /**
     * invoked by framework when the File > Save [MenuItem] is pressed.
     */
    @FXML private fun saveToFile()
    {
        val file = FileChooser()
            .apply()
            {
                title = "Save input data"
                initialDirectory = File(Paths.get(".").toAbsolutePath().normalize().toString())
            }
            .showSaveDialog(rootLayout.scene.window)

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

    /**
     * invoked by framework when the File > Load [MenuItem] is pressed.
     */
    @FXML private fun loadFromFile()
    {
        val file = FileChooser()
            .apply()
            {
                title = "Load input data"
                initialDirectory = File(Paths.get(".").toAbsolutePath().normalize().toString())
            }
            .showOpenDialog(rootLayout.scene.window)

        if (file != null)
        {
            val fileText = file
                .inputStream()
                .use {it.readBytes().let {String(it)}}

            agentsTableView.items.clear()
            // todo: allow loading from files.....right now, functionality was removed because we need to be able to save and load initial positions for robots
            agentsTableView.items.addAll(fileText
                .let {JSONArray(it)}
                .map {it as JSONObject}
                .map {it.toProblemInstance()}
                .map {AgentsTableView.RowData(it,RevisionFunctionConfigPanel(),emptySet(),Simulation.Cell.getElseMake(0,0),Behaviour.CardinalDirection.EAST,true,Color.RED,Math.random())}
                .apply {forEach {it.revisionFunctionConfigPanel.setValuesFrom(it.problemInstance.beliefRevisionStrategy)}}
                .let {ObservableListWrapper(it)})
        }
    }
}
