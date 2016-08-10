package com.github.ericytsang.research2016.announcementresolver

import com.github.ericytsang.lib.javafxutils.EditableTableView
import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import com.github.ericytsang.research2016.propositionallogic.AnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.BruteForceAnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.ComparatorBeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.OrderedAnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.Variable
import com.github.ericytsang.research2016.propositionallogic.makeFrom
import com.github.ericytsang.research2016.propositionallogic.toDnf
import com.github.ericytsang.research2016.propositionallogic.toParsableString
import com.sun.javafx.collections.ObservableListWrapper
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.TableColumn
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.nio.file.Paths
import java.util.Optional
import kotlin.concurrent.thread

class Gui:Application()
{
    private val FILE_MENU_TEXT = "File"

    private val SAVE_MENU_ITEM_TEXT = "Save"

    private val LOAD_MENU_ITEM_TEXT = "Load"

    private val WINDOW_WIDTH:Double = 700.0

    private val WINDOW_HEIGHT:Double = 400.0

    private val displayModeComboBox = DisplayModeComboBox().apply()
    {
        // configure child nodes
        valueProperty().addListener(InvalidationListener()
        {
            agentsTableView.items.forEach {it.transform = value.transform}
            agentsTableView.refresh()
        })
    }

    private val agentsTableView:EditableTableView<AgentListItem,Alert,ButtonType> = object:EditableTableView<AgentListItem,Alert,ButtonType>()
    {
        init
        {
            columns.add(TableColumn<AgentListItem,String>().apply()
            {
                text = "Initial belief state"
                cellValueFactory = Callback<TableColumn.CellDataFeatures<AgentListItem,String>,ObservableValue<String>>()
                {
                    SimpleStringProperty(it.value.initialKString)
                }
            })
            columns.add(TableColumn<AgentListItem,String>().apply()
            {
                text = "Target belief state"
                cellValueFactory = Callback<TableColumn.CellDataFeatures<AgentListItem,String>,ObservableValue<String>>()
                {
                    SimpleStringProperty(it.value.targetKString)
                }
            })
            columns.add(TableColumn<AgentListItem,String>().apply()
            {
                text = "Belief revision operator"
                cellValueFactory = Callback<TableColumn.CellDataFeatures<AgentListItem,String>,ObservableValue<String>>()
                {
                    SimpleStringProperty(it.value.operatorString)
                }
            })
            columns.add(TableColumn<AgentListItem,String>().apply()
            {
                text = "Revised belief state"
                cellValueFactory = Callback<TableColumn.CellDataFeatures<AgentListItem,String>,ObservableValue<String>>()
                {
                    SimpleStringProperty(it.value.actualKString)
                }
            })
        }

        override fun isInputCancelled(result:Optional<ButtonType>):Boolean
        {
            return result.get() != ButtonType.OK
        }

        override fun makeInputDialog(model:AgentListItem?):Alert
        {
            return InputDialog(model)
        }

        override fun tryParseInput(inputDialog:Alert):AgentListItem
        {
            inputDialog as InputDialog
            val initialK = inputDialog.initialKTextField.text.split(",").map {Proposition.makeFrom(it)}.toSet()
            val targetK = Proposition.makeFrom(inputDialog.targetKTextField.text)
            val beliefRevisionStrategy = inputDialog.operatorInputPane.beliefRevisionStrategy ?: throw IllegalArgumentException("A belief revision operation must be specified")
            return AgentListItem(AnnouncementResolutionStrategy.ProblemInstance(initialK,targetK,beliefRevisionStrategy),inputDialog.operatorInputPane,displayModeComboBox.value.transform)
        }
    }

    private val announcementLabel = Label()

    private var announcementFinderThread = thread {}

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
                Thread.sleep(100)
                isDisable = !announcementFinderThread.isAlive
            }
        }
    }

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
                        agentsTableView.items.forEach()
                        {
                            it.actualK = it.problemInstance.reviseBy(announcement)
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
                    Platform.runLater {isDisable = false}
                }
            }
        }
    }

    private val menuBarPane = MenuBar().apply()
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

        spacing = Dimens.KEYLINE_SMALL.toDouble()
        children.addAll(agentsTableView,displayModeComboBox)
    }

    private val lowerPane = HBox().apply()
    {
        HBox.setHgrow(announcementLabel,Priority.ALWAYS)
        announcementLabel.maxWidth = Double.MAX_VALUE

        spacing = Dimens.KEYLINE_SMALL.toDouble()
        children.addAll(announcementLabel,cancelButton,findAnnouncementButton)
    }

    private val rootPane = VBox().apply()
    {
        VBox.setVgrow(upperPane,Priority.ALWAYS)
        upperPane.maxHeight = Double.MAX_VALUE
        VBox.setVgrow(this,Priority.ALWAYS)
        maxHeight = Double.MAX_VALUE

        padding = Insets(Dimens.KEYLINE_SMALL.toDouble())
        spacing = Dimens.KEYLINE_SMALL.toDouble()
        children.addAll(upperPane,lowerPane)
    }

    private lateinit var primaryStage:Stage

    override fun start(primaryStage:Stage)
    {
        this.primaryStage = primaryStage
        primaryStage.scene = Scene(VBox(menuBarPane,rootPane),WINDOW_WIDTH,WINDOW_HEIGHT)
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
                .map {AgentListItem(it,RevisionFunctionConfigPanel(),displayModeComboBox.value.transform)}
                .apply {forEach {it.revisionFunctionConfigPanel.setValuesFrom(it.problemInstance.beliefRevisionStrategy)}}
                .let {ObservableListWrapper(it)}
        }
    }

    private class InputDialog(model:AgentListItem?):Alert(Alert.AlertType.NONE)
    {
        val initialKTextField = TextField()
            .apply {text = model?.problemInstance?.initialBeliefState?.map {it.toParsableString()}?.joinToString(", ") ?: ""}

        val targetKTextField = TextField()
            .apply {text = model?.problemInstance?.targetBeliefState?.toParsableString() ?: ""}

        val operatorInputPane = model?.revisionFunctionConfigPanel
            ?: RevisionFunctionConfigPanel()

        init
        {
            isResizable = true
            headerText = " "
            buttonTypes.addAll(ButtonType.CANCEL,ButtonType.OK)
            dialogPane.content = VBox().apply()
            {
                spacing = Dimens.KEYLINE_SMALL.toDouble()
                children += Label("Initial belief state:")
                children += initialKTextField
                children += Label("Target belief state:")
                children += targetKTextField
                children += Label("Belief revision operator:")
                children += operatorInputPane
            }
        }
    }

    inner private class AgentListItem(val problemInstance:AnnouncementResolutionStrategy.ProblemInstance,val revisionFunctionConfigPanel:RevisionFunctionConfigPanel,var transform:(List<Proposition>,Set<Variable>)->List<String>)
    {
        private val allVariables:Set<Variable> get()
        {
            val propositions = mutableSetOf<Proposition>()
            propositions += agentsTableView.items.flatMap {it.problemInstance.initialBeliefState}
            propositions += agentsTableView.items.map {it.problemInstance.targetBeliefState}
            return propositions.flatMap {it.variables}.toSet()
        }

        var actualK:Set<Proposition> = emptySet()

        val initialKString:String get()
        {
            return problemInstance.initialBeliefState
                .let {transform(it.toList(),allVariables)}
                .joinToString("\n")
        }

        val targetKString:String get()
        {
            return problemInstance.targetBeliefState
                .let {transform(listOf(it),allVariables)}
                .joinToString("\n")
        }

        val operatorString:String get()
        {
            return revisionFunctionConfigPanel.revisionOperatorComboBox.value.name
        }

        val actualKString:String get()
        {
            return actualK
                .let {transform(it.toList(),allVariables)}
                .joinToString("\n")
        }
    }
}
