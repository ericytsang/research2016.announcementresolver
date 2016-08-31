package com.github.ericytsang.research2016.announcementresolver.window

import com.github.ericytsang.lib.collections.KeyedChange
import com.github.ericytsang.lib.concurrent.Future
import com.github.ericytsang.lib.concurrent.future
import com.github.ericytsang.lib.javafxutils.JavafxUtils
import com.github.ericytsang.lib.oopatterns.Change
import com.github.ericytsang.lib.oopatterns.addAndUpdate
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.announcementresolver.guicomponent.AgentsTableView
import com.github.ericytsang.research2016.announcementresolver.guicomponent.DisplayModeComboBox
import com.github.ericytsang.research2016.announcementresolver.guicomponent.ObstacleTableView
import com.github.ericytsang.research2016.announcementresolver.persist.AgentsSaveFileParser
import com.github.ericytsang.research2016.announcementresolver.simulation.AgentController
import com.github.ericytsang.research2016.announcementresolver.simulation.Obstacle
import com.github.ericytsang.research2016.announcementresolver.simulation.VirtualAgentController
import com.github.ericytsang.research2016.announcementresolver.simulation.Wall
import com.github.ericytsang.research2016.propositionallogic.BruteForceAnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.ComparatorBeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.OrderedAnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.toDnf
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
import javafx.scene.control.MenuItem
import javafx.scene.control.Label
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
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
     * window used by user to map variables to robot behaviours.
     */
    private val behaviouralDictionaryWindowController = DefinitionsWindowController.new()

    private val obstacleWindowController = ObstacleWindowController.new()

    private val simulationWindowController = SimulatorWindowController.new()

    /**
     * a map of agent ids to their corresponding agent controller.
     */
    private val agentControllers = LinkedHashMap<Double,AgentController>()

    /**
     * the future used to calculate the announcement to announce to agents.
     */
    private var announcementFinder:Future<Proposition?> = future<Proposition?> {throw IllegalStateException("no")}
        set(value)
        {
            // unregister listeners (if any) from previous future
            field.status.observers.clear()

            // set the backing field to the given value
            field = value

            // add listeners to the new future
            field.status.addAndUpdate(Change.Observer.new()
            {
                // set the announcement label text according to the status of the new future
                val labelText = when(it.newValue)
                {
                    Future.Status.PENDING -> "finding announcement..."
                    Future.Status.SUCCESS -> field.await()?.toDnf()?.toString() ?: "no announcement found..."
                    Future.Status.FAILURE -> field.exception.value!!.message
                }

                Platform.runLater {announcementLabel.text = labelText}
            })
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
                val threadIsAlive = announcementFinder.workerThread.isAlive
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
        behaviouralDictionaryWindowController.definitionsTableView.items.addListener(InvalidationListener()
        {
            agentControllers.values.forEach()
            {
                it.uploadBehaviourDictionary(behaviouralDictionaryWindowController
                    .definitionsTableView.items
                    .map {it.proposition to it.behavior})
            }
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
                .putAll(obstacleWindowController.obstacleTableView.items.associate {Wall(it.cell1,it.cell2) to emptySet<Simulation.Cell>()})
        })

        /*
         * when obstacles in the simulation change, upload the information to
         * the agents.
         */
        run()
        {
            var knownOccupiedCells = emptySet<Simulation.Cell>()
            simulationWindowController.simulation.entityToCellsMap.observers += KeyedChange.Observer.new()
            {
                val occupiedCells = simulationWindowController
                    .simulation.entityToCellsMap
                    .filter {it.key is Obstacle}
                    .flatMap {it.value}.toSet()

                if (occupiedCells != knownOccupiedCells)
                {
                    knownOccupiedCells = occupiedCells
                    agentControllers.values.forEach()
                    {
                        it.uploadObstacles(occupiedCells)
                    }
                }
            }
        }

        /*
         * when the agent table view is modified, update the corresponding
         * [AgentController] objects in [agentControllers].
         */
        agentsTableView.items.addListener(InvalidationListener()
        {
            // release resources and forget about existing agent controllers
            // that no longer have a corresponding entry in the table view.
            run()
            {
                val existingAgentIds = agentsTableView.items.map {it.agentId}.toSet()
                agentControllers.filter {it.key !in existingAgentIds}.forEach()
                {
                    val (agentId,agentController) = it

                    // remove the agent controller from the agentController map
                    agentControllers.remove(agentId)

                    // shutdown the agent controller
                    agentController.shutdown()

                    // detach listeners from the agent controller
                    agentController.isConnected.observers.clear()

                    // remove the agent controller from the simulation
                    simulationWindowController.simulation.entityToCellsMap.remove(agentController)
                }
            }

            // associate new agent controllers with entries in the table view
            // that are not currently associated with any agent controller.
            run()
            {
                agentsTableView.items.filter {it.agentId !in agentControllers.keys}.forEach()
                {
                    rowData ->

                    // todo: instantiate either a virtual or actual robot controller based on row data
                    val agentController = VirtualAgentController()

                    // add the new agent to the agentControllers map
                    agentControllers += rowData.agentId to agentController

                    // attach listeners to the agent controller
                    agentController.isConnected.addAndUpdate(Change.Observer.new()
                    {
                        Platform.runLater()
                        {
                            // if the row for this agent still exists, update it
                            val itemIndex = agentsTableView.items.indexOfFirst {it.agentId == rowData.agentId}
                            if (itemIndex != -1)
                            {
                                val selectedIndex = agentsTableView.selectionModel.selectedIndex
                                agentsTableView.items[itemIndex] = agentsTableView.items[itemIndex]
                                    .copy(isConnected = it.newValue)
                                if (selectedIndex != -1) agentsTableView.selectionModel.select(selectedIndex)
                            }
                        }
                    })

                    // upload necessary data to the agent controller
                    agentController.bodyColor = rowData.color
                    agentController.position = Point2D(
                        rowData.newPosition.x.toDouble(),
                        rowData.newPosition.y.toDouble())
                    agentController.direction = rowData.newDirection.angle
                    agentController.uploadBeliefState(rowData
                        .problemInstance.initialBeliefState)
                    agentController.uploadBeliefRevisionStrategy(rowData
                        .problemInstance.beliefRevisionStrategy)
                    agentController.uploadBehaviourDictionary(behaviouralDictionaryWindowController
                        .definitionsTableView.items
                        .map {it.proposition to it.behavior})
                    agentController.uploadObstacles(simulationWindowController
                        .simulation.entityToCellsMap
                        .filter {it.key is Obstacle}
                        .flatMap {it.value}.toSet())

                    // add the agent controller to the simulation
                    simulationWindowController.simulation.entityToCellsMap.put(agentController,emptySet())
                }
            }

            // update existing agent controllers that correspond to an existing
            // entry in the table view that have been manually modified by the
            // user.
            run()
            {
                agentsTableView.items.forEach()
                {
                    rowData ->

                    // get the corresponding agent controller. it must exist
                    // because any non-existent agent controllers should
                    // have been created in the previous run block above
                    val agentController = agentControllers[rowData.agentId]!!

                    // update agent controller from edited row data
                    agentController.bodyColor = rowData.color
                    agentController.uploadBeliefState(rowData
                        .problemInstance.initialBeliefState)
                    agentController.uploadBeliefRevisionStrategy(rowData
                        .problemInstance.beliefRevisionStrategy)

                    // setting the position and direction of the robot to
                    // the user-specified one if it exists
                    if (rowData.shouldJumpToPosition)
                    {
                        agentController.position = rowData.newPosition.let {Point2D(it.x.toDouble(),it.y.toDouble())}
                        agentController.direction = rowData.newDirection.angle

                        // unset flag
                        rowData.shouldJumpToPosition = false
                    }
                }
            }
        })

        // add some default walls about the simulation to prevent agents from leaving
        obstacleWindowController.obstacleTableView.items.addAll(
            ObstacleTableView.RowData(Simulation.Cell.getElseMake(8,8),Simulation.Cell.getElseMake(-8,8)),
            ObstacleTableView.RowData(Simulation.Cell.getElseMake(8,8),Simulation.Cell.getElseMake(8,-8)),
            ObstacleTableView.RowData(Simulation.Cell.getElseMake(-8,-8),Simulation.Cell.getElseMake(-8,8)),
            ObstacleTableView.RowData(Simulation.Cell.getElseMake(-8,-8),Simulation.Cell.getElseMake(8,-8)))
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
        announcementFinder.workerThread.interrupt()
    }

    /**
     * invoked by framework when [findAnnouncementButton] is clicked.
     */
    @FXML private fun beginAnnouncementFinding()
    {
        // display loading...
        findAnnouncementButton.isDisable = true

        announcementFinder = future()
        {
            return@future try
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

                // display the revised belief states
                if (announcement != null)
                {
                    val newListItems = agentsTableView.items
                        .map {it.copy(actualK = it.problemInstance.reviseBy(announcement))}

                    Platform.runLater()
                    {
                        agentsTableView.items.setAll(newListItems)
                    }
                }

                announcement
            }
            catch (ex:InterruptedException)
            {
                throw RuntimeException("operation cancelled")
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
        if (agentsTableView.items.all {it.actualK.isNotEmpty()} && announcementFinder.status.value == Future.Status.SUCCESS)
        {
            agentControllers.values.forEach()
            {
                it.uploadSentenceForBeliefRevision(announcementFinder.await()!!)
            }

            val newListItems = agentsTableView.items.map()
            {
                existingRowData ->
                val newProblemInstance = existingRowData.problemInstance.copy(initialBeliefState = existingRowData.actualK)
                existingRowData.copy(problemInstance = newProblemInstance,actualK = emptySet())
            }

            val selectedIndex = agentsTableView.selectionModel.selectedIndex
            agentsTableView.items.setAll(newListItems)
            agentsTableView.selectionModel.select(selectedIndex)
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
                title = "Save Input Data"
                initialDirectory = File(Paths.get(".").toAbsolutePath().normalize().toString())
                extensionFilters += FileChooser.ExtensionFilter("agents","*.agents")
                extensionFilters += FileChooser.ExtensionFilter("json","*.json")
                extensionFilters += FileChooser.ExtensionFilter("any","*")
            }
            .showSaveDialog(stage.scene.window)

        if (file != null)
        {
            try
            {
                val agents = agentsTableView.items
                    .map {AgentsSaveFileParser.Agent(it.problemInstance,it.newPosition,it.newDirection,it.color)}
                AgentsSaveFileParser.save(file,agents)
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
                extensionFilters += FileChooser.ExtensionFilter("agents","*.agents")
                extensionFilters += FileChooser.ExtensionFilter("json","*.json")
                extensionFilters += FileChooser.ExtensionFilter("any","*")
            }
            .showOpenDialog(stage.scene.window)

        // if a file was chosen, load data from it
        if (file != null)
        {
            try
            {
                // load agents from the file
                val parsedAgents = AgentsSaveFileParser.load(file)
                val agents = parsedAgents
                    .map {AgentsTableView.RowData(Math.random(),false,it.problemInstance,emptySet(),it.color,it.position,it.direction,true)}

                // try to reload from the file again if generated agent IDs are not unique
                if (agents.map {it.agentId}.toSet().size != agents.size)
                {
                    throw IllegalArgumentException("failed to generate unique agent IDs for agents")
                }

                // add them to the table view
                agentsTableView.items.setAll(agents)
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
