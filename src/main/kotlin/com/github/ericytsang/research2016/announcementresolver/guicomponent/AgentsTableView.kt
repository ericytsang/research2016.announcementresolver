package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.github.ericytsang.lib.javafxutils.EditableTableView
import com.github.ericytsang.lib.javafxutils.ValidatableTextField
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.announcementresolver.NamedValue
import com.github.ericytsang.research2016.announcementresolver.simulation.Behaviour
import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import com.github.ericytsang.research2016.propositionallogic.AnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.Variable
import com.github.ericytsang.research2016.propositionallogic.makeFrom
import com.github.ericytsang.research2016.propositionallogic.toParsableString
import javafx.beans.InvalidationListener
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.util.Callback

class AgentsTableView():EditableTableView<AgentsTableView.RowData>()
{
    companion object
    {
        /**
         * the number attempts the dialog box tried to generate a unique row id
         * when creating a new row (not editing an existing one)
         */
        const val NUM_ATTEMPTS_TO_GENERATE_NEW_ROW_ID = 100
    }

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

                // parse initial K, target K and belief revision strategy
                val initialK = inputDialog.initialKTextField.text.split(",").map {Proposition.makeFrom(it)}.toSet()
                val targetK = Proposition.makeFrom(inputDialog.targetKTextField.text)
                val beliefRevisionStrategy = inputDialog.operatorInputPane.beliefRevisionStrategy ?: throw IllegalArgumentException("A belief revision operation must be specified")

                // get the user's positioning and direction input. either the x
                // position, y position and direction input are all there, or
                // they are all not there...
                val newPosition = Simulation.Cell.getElseMake(
                    inputDialog.initialXPositionTextField.text.toInt(),
                    inputDialog.initialYPositionTextField.text.toInt())
                val newDirection = inputDialog.directionComboBox.value.value

                val robotColor = inputDialog.colorComboBox.value.value
                val shouldJumpToPosition = inputDialog.jumpToInitialPositionCheckBox.isSelected

                // try to use the row id from the previous input...if it doesn't exist, try to generate a new one that hopefully will not collide with another row...
                val robotId = previousInput?.agentId ?: run()
                {
                    var remainingAttempts = NUM_ATTEMPTS_TO_GENERATE_NEW_ROW_ID
                    while (remainingAttempts > 0)
                    {
                        val potentialId = Math.random()
                        if (items.all {it.agentId != potentialId})
                        {
                            return@run potentialId
                        }
                        --remainingAttempts
                    }
                    throw IllegalArgumentException("failed to generate unique ID for row after $NUM_ATTEMPTS_TO_GENERATE_NEW_ROW_ID attempts")
                }

                // done parsing...construct and return the row data object
                return RowData(AnnouncementResolutionStrategy.ProblemInstance(initialK,targetK,beliefRevisionStrategy),inputDialog.operatorInputPane,emptySet(),newPosition,newDirection,shouldJumpToPosition,robotColor,robotId)
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
    data class RowData(
        val problemInstance:AnnouncementResolutionStrategy.ProblemInstance,
        val revisionFunctionConfigPanel:RevisionFunctionConfigPanel,
        val actualK:Set<Proposition>,
        val newPosition:Simulation.Cell,
        val newDirection:Behaviour.CardinalDirection,
        val shouldJumpToInitialPosition:Boolean,
        val color:Color,
        val agentId:Double)

    /**
     * dialog shown when inserting new entries into the [AgentsTableView] or
     * editing existing entries.
     */
    inner private class InputDialog(val model:RowData?):Alert(AlertType.NONE)
    {
        val initialKTextField = TextField().apply()
        {
            text = model?.problemInstance?.initialBeliefState?.map {it.toParsableString()}?.joinToString(", ") ?: ""
        }

        val targetKTextField = TextField().apply()
        {
            text = model?.problemInstance?.targetBeliefState?.toParsableString() ?: ""
        }

        val colorComboBox = ComboBox<NamedValue<Color>>().apply()
        {
            items.add(NamedValue("Red",Color.RED))
            items.add(NamedValue("Green",Color.GREEN))
            items.add(NamedValue("Blue",Color.BLUE))
            items.add(NamedValue("Yellow",Color.YELLOW))
            items.add(NamedValue("Cyan",Color.CYAN))
            items.add(NamedValue("Magenta",Color.MAGENTA))
            value = items.find {it.value == model?.color} ?: items.first()
        }

        val initialXPositionTextField = ValidatableTextField.makeIntegerTextField().apply()
        {
            text = model?.newPosition?.x?.toString() ?: "0"
        }

        val initialYPositionTextField = ValidatableTextField.makeIntegerTextField().apply()
        {
            text = model?.newPosition?.y?.toString() ?: "0"
        }

        val directionComboBox = ComboBox<NamedValue<Behaviour.CardinalDirection>>().apply()
        {
            items.add(NamedValue("North",Behaviour.CardinalDirection.NORTH))
            items.add(NamedValue("East",Behaviour.CardinalDirection.EAST))
            items.add(NamedValue("South",Behaviour.CardinalDirection.SOUTH))
            items.add(NamedValue("West",Behaviour.CardinalDirection.WEST))
            value = items.find {it.value == model?.newDirection} ?: items.first()
        }

        val jumpToInitialPositionCheckBox = CheckBox().apply()
        {
            isSelected = model?.shouldJumpToInitialPosition ?: true
            val listener = InvalidationListener()
            {
                initialXPositionTextField.isDisable = !isSelected
                initialYPositionTextField.isDisable = !isSelected
                directionComboBox.isDisable = !isSelected
            }
            selectedProperty().addListener(listener)
            listener.invalidated(selectedProperty())
        }

        val operatorInputPane = model?.revisionFunctionConfigPanel
            ?: RevisionFunctionConfigPanel()

        init
        {
            dialogPane.minWidth = 400.0
            dialogPane.minHeight = 600.0
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
                children += Label("Agent color:")
                children += colorComboBox
                children += Label("Jump to specified position:")
                children += jumpToInitialPositionCheckBox
                children += Label("X Position:")
                children += initialXPositionTextField
                children += Label("Y Position:")
                children += initialYPositionTextField
                children += Label("Direction:")
                children += directionComboBox
                children += Label("Belief revision operator:")
                children += operatorInputPane
            }
        }
    }
}
