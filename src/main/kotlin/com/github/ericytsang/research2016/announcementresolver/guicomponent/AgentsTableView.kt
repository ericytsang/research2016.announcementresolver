package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.github.ericytsang.lib.javafxutils.EditableTableView
import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import com.github.ericytsang.research2016.propositionallogic.AnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.Variable
import com.github.ericytsang.research2016.propositionallogic.makeFrom
import com.github.ericytsang.research2016.propositionallogic.toParsableString
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventHandler
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.util.Callback

class AgentsTableView():EditableTableView<AgentsTableView.RowData>()
{
    init
    {
        // add columns to the table view
        columns += TableColumn<RowData,String>().apply()
        {
            text = "Initial belief state"
            cellValueFactory = Callback()
            {
                it.value.problemInstance.initialBeliefState
                    .let {beliefStateToString(it.toList(),allVariables)}
                    .joinToString("\n")
                    .let {SimpleStringProperty(it)}
            }
        }
        columns += TableColumn<RowData,String>().apply()
        {
            text = "Target belief state"
            cellValueFactory = Callback()
            {
                it.value.problemInstance.targetBeliefState
                    .let {beliefStateToString(listOf(it),allVariables)}
                    .joinToString("\n")
                    .let {SimpleStringProperty(it)}
            }
        }
        columns += TableColumn<RowData,String>().apply()
        {
            text = "Belief revision operator"
            cellValueFactory = Callback()
            {
                it.value.revisionFunctionConfigPanel.revisionOperatorComboBox.value.name
                    .let {SimpleStringProperty(it)}
            }
        }
        columns += TableColumn<RowData,String>().apply()
        {
            text = "Revised belief state"
            cellValueFactory = Callback()
            {
                it.value.actualK
                    .let {beliefStateToString(it.toList(),allVariables)}
                    .joinToString("\n")
                    .let {SimpleStringProperty(it)}
            }
        }

        // set the column resize policy
        columnResizePolicy = CONSTRAINED_RESIZE_POLICY
    }

    override fun createOrUpdateItem(previousInput:RowData?):RowData?
    {
        val inputDialog = InputDialog(previousInput)
        while (inputDialog.showAndWait().get() == ButtonType.OK)
        {
            try
            {
                // todo: better error messages
                val initialK = inputDialog.initialKTextField.text.split(",").map {Proposition.makeFrom(it)}.toSet()
                val targetK = Proposition.makeFrom(inputDialog.targetKTextField.text)
                val beliefRevisionStrategy = inputDialog.operatorInputPane.beliefRevisionStrategy ?: throw IllegalArgumentException("A belief revision operation must be specified")
                return RowData(AnnouncementResolutionStrategy.ProblemInstance(initialK,targetK,beliefRevisionStrategy),inputDialog.operatorInputPane,emptySet())
            }
            catch (ex:Exception)
            {
                val alert = Alert(Alert.AlertType.ERROR)
                alert.title = "Invalid Input"
                alert.headerText = "Invalid input format."
                alert.contentText = ex.message
                alert.showAndWait()
            }
        }
        return null
    }

    /**
     * takes a belief state as input and returns how it should be displayed on
     * the table view. each element in the returned list is displayed on
     * separate lines.
     */
    var beliefStateToString:(List<Proposition>,Set<Variable>)->List<String> = fun(beliefState:List<Proposition>,vocabulary:Set<Variable>):List<String>
    {
        return beliefState.map {it.toString()}
    }

        set(value)
        {
            field = value
            refresh()
        }

    /**
     * returns a set of all unique [Variable] instances in [items].
     */
    private val allVariables:Set<Variable> get()
    {
        val propositions = mutableSetOf<Proposition>()
        propositions += items.flatMap {it.problemInstance.initialBeliefState}
        propositions += items.map {it.problemInstance.targetBeliefState}
        return propositions.flatMap {it.variables}.toSet()
    }

    /**
     * each [RowData] instance can be represented as a row in [AgentsTableView].
     */
    // todo: allow users to specify an initial direction for the agent
    // todo: allow users to specify an initial position for the agent
    // todo: allow users to specify a color for the robot
    // todo: generate a secret ID that doesn't get shown to the user.....so the program can tell which rows were previously existing rows that were edited, which rows were removed, and which ones were added
    data class RowData(
        val problemInstance:AnnouncementResolutionStrategy.ProblemInstance,
        val revisionFunctionConfigPanel:RevisionFunctionConfigPanel,
        val actualK:Set<Proposition>)

    /**
     * dialog shown when inserting new entries into the [AgentsTableView] or
     * editing existing entries.
     */
    inner private class InputDialog(model:RowData?):Alert(AlertType.NONE)
    {
        val initialKTextField = TextField()
            .apply {text = model?.problemInstance?.initialBeliefState?.map {it.toParsableString()}?.joinToString(", ") ?: ""}

        val targetKTextField = TextField()
            .apply {text = model?.problemInstance?.targetBeliefState?.toParsableString() ?: ""}

        val operatorInputPane = model?.revisionFunctionConfigPanel
            ?: RevisionFunctionConfigPanel()

        init
        {
            dialogPane.minWidth = 400.0
            dialogPane.minHeight = 400.0
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
}
