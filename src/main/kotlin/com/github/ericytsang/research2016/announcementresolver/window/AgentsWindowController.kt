package com.github.ericytsang.research2016.announcementresolver.window

import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.announcementresolver.guicomponent.AgentsTableView
import com.github.ericytsang.research2016.announcementresolver.guicomponent.DisplayModeComboBox
import com.github.ericytsang.research2016.announcementresolver.guicomponent.RevisionFunctionConfigPanel
import com.github.ericytsang.research2016.announcementresolver.simulation.AgentController
import com.github.ericytsang.research2016.announcementresolver.simulation.Behaviour
import com.github.ericytsang.research2016.announcementresolver.simulation.CanvasRenderer
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

class AgentsWindowController:Initializable
{
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

    private lateinit var behaviouralDictionaryWindowController:BehaviouralDictionaryWindowController

    /**
     * window used by user to map variables to robot behaviours.
     */
    private val behaviouralDictionaryWindow = Stage().apply()
    {
        val loader = FXMLLoader(this@AgentsWindowController.javaClass.classLoader.getResource("behavioraldictionarywindow.fxml"))
        scene = Scene(loader.load<Parent>())
        title = "Behavioural Dictionary"
        behaviouralDictionaryWindowController = loader.getController()
    }

    private lateinit var obstacleWindowController:ObstacleWindowController

    private val obstacleWindow = Stage().apply()
    {
        val loader = FXMLLoader(this@AgentsWindowController.javaClass.classLoader.getResource("obstaclewindow.fxml"))
        scene = Scene(loader.load())
        title = "Obstacles"
        obstacleWindowController = loader.getController()
    }

    private lateinit var simulationWindowController:SimulatorWindowController

    private val simulationWindow = Stage().apply()
    {
        val loader = FXMLLoader(this@AgentsWindowController.javaClass.classLoader.getResource("simulatorwindow.fxml"))
        scene = Scene(loader.load())
        title = "Simulation"
        simulationWindowController = loader.getController()
    }

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
         * when the behaviouralDictionaryWindow's tableview items are changed,
         * update the dictionaries in the agents as well
         */
        behaviouralDictionaryWindowController.behaviouralDictionaryTableView.items.addListener(InvalidationListener()
        {
            val oldItems = agentsTableView.items.toList()
            agentsTableView.items.setAll(oldItems)
        })

        /*
         * [toggleDictionaryWindowCheckMenuItem] displays a check mark beside
         * itself when [behaviouralDictionaryWindow] is showing; no check mark is displayed
         * otherwise.
         */
        behaviouralDictionaryWindow.scene.window.onShown = EventHandler()
        {
            toggleDictionaryWindowCheckMenuItem.isSelected = true
        }
        behaviouralDictionaryWindow.scene.window.onHidden = EventHandler()
        {
            toggleDictionaryWindowCheckMenuItem.isSelected = false
        }

        /*
         * [toggleObstacleWindowCheckMenuItem] displays a check mark beside
         * itself when [obstacleWindow] is showing; no check mark is displayed
         * otherwise.
         */
        obstacleWindow.scene.window.onShown = EventHandler()
        {
            toggleObstacleWindowCheckMenuItem.isSelected = true
        }
        obstacleWindow.scene.window.onHidden = EventHandler()
        {
            toggleObstacleWindowCheckMenuItem.isSelected = false
        }

        /*
         * [toggleSimulationWindowCheckMenuItem] displays a check mark beside
         * itself when [simulationWindow] is showing; no check mark is displayed
         * otherwise.
         */
        simulationWindow.scene.window.onShown = EventHandler()
        {
            toggleSimulationWindowCheckMenuItem.isSelected = true
        }
        simulationWindow.scene.window.onHidden = EventHandler()
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
                agentController.setBehaviourDictionary(behaviouralDictionaryWindowController.behaviouralDictionaryTableView.items.associate {it.variable to it.behavior})
                agentController.bodyColor = rowData.color

                // setting the position and direction of the robot to the user-specified one if it exists
                if (rowData.shouldJumpToInitialPosition)
                {
                    agentController.position = rowData.newPosition.let {CanvasRenderer.Position(it.x.toDouble(),it.y.toDouble())}
                    agentController.direction = when (rowData.newDirection)
                    {
                        Behaviour.CardinalDirection.NORTH -> 270.0
                        Behaviour.CardinalDirection.EAST -> 0.0
                        Behaviour.CardinalDirection.SOUTH -> 90.0
                        Behaviour.CardinalDirection.WEST -> 180.0
                    }
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
        behaviouralDictionaryWindow.hide()
        obstacleWindow.hide()
        simulationWindow.hide()
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
        if (behaviouralDictionaryWindow.isShowing)
        {
            behaviouralDictionaryWindow.hide()
        }
        else
        {
            behaviouralDictionaryWindow.show()
        }
    }

    /**
     * invoked by framework when the Window > Obstacles [CheckMenuItem] pressed.
     */
    @FXML private fun toggleObstacleWindow()
    {
        if (obstacleWindow.isShowing)
        {
            obstacleWindow.hide()
        }
        else
        {
            obstacleWindow.show()
        }
    }

    /**
     * invoked by framework when the Window > Simulation [CheckMenuItem]
     * pressed.
     */
    @FXML private fun toggleSimulationWindow()
    {
        if (simulationWindow.isShowing)
        {
            simulationWindow.hide()
        }
        else
        {
            simulationWindow.show()
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
