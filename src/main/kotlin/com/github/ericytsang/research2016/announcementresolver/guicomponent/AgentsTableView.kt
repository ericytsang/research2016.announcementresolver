package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.github.ericytsang.lib.javafxutils.EditableTableView
import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import com.github.ericytsang.research2016.propositionallogic.AnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.Variable
import com.github.ericytsang.research2016.propositionallogic.makeFrom
import com.github.ericytsang.research2016.propositionallogic.toParsableString
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.util.Callback
import java.util.Optional
import kotlin.concurrent.thread

class AgentsTableView():EditableTableView<AgentsTableView.RowData,Alert,ButtonType>()
{
    init
    {
        // add columns to the table view
        columns.add(TableColumn<RowData,String>().apply()
        {
            text = "Initial belief state"
            cellValueFactory = Callback<TableColumn.CellDataFeatures<RowData,String>,ObservableValue<String>>()
            {
                it.value.problemInstance.initialBeliefState
                    .let {beliefStateToString(it.toList(),allVariables)}
                    .joinToString("\n")
                    .let {SimpleStringProperty(it)}
            }
        })
        columns.add(TableColumn<RowData,String>().apply()
        {
            text = "Target belief state"
            cellValueFactory = Callback<TableColumn.CellDataFeatures<RowData,String>,ObservableValue<String>>()
            {
                it.value.problemInstance.targetBeliefState
                    .let {beliefStateToString(listOf(it),allVariables)}
                    .joinToString("\n")
                    .let {SimpleStringProperty(it)}
            }
        })
        columns.add(TableColumn<RowData,String>().apply()
        {
            text = "Belief revision operator"
            cellValueFactory = Callback<TableColumn.CellDataFeatures<RowData,String>,ObservableValue<String>>()
            {
                it.value.revisionFunctionConfigPanel.revisionOperatorComboBox.value.name
                    .let {SimpleStringProperty(it)}
            }
        })
        columns.add(TableColumn<RowData,String>().apply()
        {
            text = "Revised belief state"
            cellValueFactory = Callback<TableColumn.CellDataFeatures<RowData,String>,ObservableValue<String>>()
            {
                it.value.actualK
                    .let {beliefStateToString(it.toList(),allVariables)}
                    .joinToString("\n")
                    .let {SimpleStringProperty(it)}
            }
        })

        // set columns to appropriate width after layout dimensions have settled
        thread()
        {
            Thread.sleep(1000)
            Platform.runLater()
            {
                columns.forEach()
                {
                    it.prefWidth = (width/columns.size)
                }
            }
        }
    }

    override fun isInputCancelled(result:Optional<ButtonType>):Boolean
    {
        return result.get() != ButtonType.OK
    }

    override fun makeInputDialog(model:RowData?):Alert
    {
        return InputDialog(model)
    }

    override fun tryParseInput(inputDialog:Alert):RowData
    {
        inputDialog as InputDialog
        val initialK = inputDialog.initialKTextField.text.split(",").map {Proposition.makeFrom(it)}.toSet()
        val targetK = Proposition.makeFrom(inputDialog.targetKTextField.text)
        val beliefRevisionStrategy = inputDialog.operatorInputPane.beliefRevisionStrategy ?: throw IllegalArgumentException("A belief revision operation must be specified")
        return RowData(AnnouncementResolutionStrategy.ProblemInstance(initialK,targetK,beliefRevisionStrategy),inputDialog.operatorInputPane,emptySet())
    }

    var beliefStateToString:(List<Proposition>,Set<Variable>)->List<String> = fun(beliefState:List<Proposition>,vocabulary:Set<Variable>):List<String>
    {
        return beliefState.map {it.toString()}
    }

        set(value)
        {
            field = value
            refresh()
        }

    data class RowData(
        val problemInstance:AnnouncementResolutionStrategy.ProblemInstance,
        val revisionFunctionConfigPanel:RevisionFunctionConfigPanel,
        val actualK:Set<Proposition>)

    private val allVariables:Set<Variable> get()
    {
        val propositions = mutableSetOf<Proposition>()
        propositions += items.flatMap {it.problemInstance.initialBeliefState}
        propositions += items.map {it.problemInstance.targetBeliefState}
        return propositions.flatMap {it.variables}.toSet()
    }

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
