package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.github.ericytsang.lib.javafxutils.EditableTableView
import com.github.ericytsang.lib.javafxutils.JavafxUtils
import com.github.ericytsang.lib.javafxutils.NamedValue
import com.github.ericytsang.lib.javafxutils.ValidatableTextField
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.announcementresolver.simulation.Behaviour
import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import com.github.ericytsang.research2016.propositionallogic.AnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.Variable
import com.github.ericytsang.research2016.propositionallogic.makeFrom
import com.github.ericytsang.research2016.propositionallogic.toParsableString
import javafx.beans.InvalidationListener
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TextField
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.util.Callback

/**
 * allows the user to view and modify a collection of [AgentsTableView.RowData]
 * objects.
 */
class AgentsTableView():EditableTableView<AgentsTableView.RowData>()
{
    companion object
    {
        /**
         * names of columns in the table view.
         */
        private const val COLUMN_NAME_CONNECTIVITY:String = "Online"
        private const val COLUMN_NAME_CURRENT_K:String = "Current belief state"
        private const val COLUMN_NAME_TARGET_K:String = "Target belief state"
        private const val COLUMN_NAME_BELIEF_REVISION_OPERATOR:String = "Belief revision operator"
        private const val COLUMN_NAME_REVISED_K:String = "Revised belief state"

        /**
         * background of cells in the connectivity column based on their values.
         */
        private val CONNECTIVITY_CELL_CONNECTED_BACKGROUND:Background = Background(BackgroundFill(Color(0.5647059,0.93333334,0.5647059,0.75),CornerRadii.EMPTY,Insets.EMPTY))
        private val CONNECTIVITY_CELL_DISCONNECTED_BACKGROUND:Background = Background(BackgroundFill(Color(1.0,0.7137255,0.75686276,0.75),CornerRadii.EMPTY,Insets.EMPTY))

        /**
         * text used in the input dialog box shown when creating a new list item
         * or editing an existing one.
         */
        private const val DIALOG_TITLE_ADD:String = "Add New Agent"
        private const val DIALOG_TITLE_EDIT:String = "Edit Existing Agent"
        private const val DIALOG_LABEL_CURRENT_K:String = "Current belief state:"
        private const val DIALOG_LABEL_TARGET_K:String = "Target belief state:"
        private const val DIALOG_LABEL_COLOR:String = "Agent color:"
        private const val DIALOG_LABEL_SHOULD_JUMP:String = "Jump to specified position:"
        private const val DIALOG_LABEL_JUMP_X:String = "X Position:"
        private const val DIALOG_LABEL_JUMP_Y:String = "Y Position:"
        private const val DIALOG_LABEL_JUMP_DIRECTION:String = "Direction:"
        private const val DIALOG_LABEL_BELIEF_REVISION_OPERATOR:String = "Belief revision operator:"

        /**
         * initial dimensions of the input dialog box.
         */
        private const val DIALOG_WIDTH:Double = 400.0
        private const val DIALOG_HEIGHT:Double = 700.0

        /**
         * the options available in the input dialog for setting the color of
         * the agent.
         */
        private val AGENT_COLOR_OPTIONS:List<NamedValue<Color>> = listOf(
            NamedValue("Red",Color.RED),
            NamedValue("Green",Color.GREEN),
            NamedValue("Blue",Color.BLUE),
            NamedValue("Yellow",Color.YELLOW),
            NamedValue("Cyan",Color.CYAN),
            NamedValue("Magenta",Color.MAGENTA))
    }

    init
    {
        // add columns to the table view
        columns += TableColumn<RowData,String>().apply()
        {
            text = COLUMN_NAME_CONNECTIVITY
            cellFactory = Callback()
            {
                object:TableCell<RowData,String>()
                {
                    override fun updateItem(item:String?,empty:Boolean)
                    {
                        super.updateItem(item,empty)

                        if (empty || item == null)
                        {
                            text = null
                            graphic = null
                        }
                        else
                        {
                            text = item.toString()
                        }

                        when((tableRow.item as RowData?)?.isConnected)
                        {
                            true ->
                            {
                                background = CONNECTIVITY_CELL_CONNECTED_BACKGROUND
                            }
                            false ->
                            {
                                background = CONNECTIVITY_CELL_DISCONNECTED_BACKGROUND
                            }
                            null ->
                            {
                                background = null
                            }
                        }
                    }
                }
            }
            cellValueFactory = Callback()
            {
                if (it.value.isConnected)
                {
                    "yes"
                }
                else
                {
                    "no"
                }.let {SimpleStringProperty(it)}
            }
        }
        columns += TableColumn<RowData,String>().apply()
        {
            text = COLUMN_NAME_CURRENT_K
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
            text = COLUMN_NAME_TARGET_K
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
            text = COLUMN_NAME_BELIEF_REVISION_OPERATOR
            cellValueFactory = Callback()
            {
                it.value.problemInstance.beliefRevisionStrategy.friendlyName
                    .let {SimpleStringProperty(it)}
            }
        }
        columns += TableColumn<RowData,String>().apply()
        {
            text = COLUMN_NAME_REVISED_K
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

                // try to use the row id from the previous input...if it doesn't
                // exist, try to generate a new one that hopefully will not
                // collide with another row...
                val robotId = previousInput?.agentId ?: run()
                {
                    val potentialId = Math.random()
                    if (items.all {it.agentId != potentialId})
                    {
                        return@run potentialId
                    }
                    else
                    {
                        throw IllegalArgumentException("failed to generate unique ID for agent. please try again.")
                    }
                }

                // done parsing...construct and return the row data object
                return RowData(robotId,false,
                    AnnouncementResolutionStrategy.ProblemInstance(initialK,targetK,beliefRevisionStrategy),
                    emptySet(),robotColor,newPosition,newDirection,shouldJumpToPosition,true)
            }
            catch (ex:Exception)
            {
                JavafxUtils.showErrorDialog("Invalid Input","Invalid input format.",ex)
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
     * each [RowData] instance can be represented as a row in [AgentsTableView].
     */
    data class RowData(
        val agentId:Double,
        val isConnected:Boolean,
        val problemInstance:AnnouncementResolutionStrategy.ProblemInstance,
        val actualK:Set<Proposition>,
        val color:Color,
        val newPosition:Simulation.Cell,
        val newDirection:Behaviour.CardinalDirection,

        /**
         * set to true to make the agent controller set the position and
         * direction of the agent to [newPosition] and [newDirection] respectively.
         */
        var shouldJumpToPosition:Boolean,

        /**
         * true to make the agent controller update its agent with the
         * information in this instance.
         */
        var isManuallyEdited:Boolean)

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
     * dialog shown when inserting new entries into the [AgentsTableView] or
     * editing existing entries.
     */
    private inner class InputDialog(val model:RowData?):Alert(AlertType.NONE)
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
            items.addAll(AGENT_COLOR_OPTIONS)
            value = items.find {it.value == model?.color} ?: items.first()
        }

        val initialXPositionTextField = ValidatableTextField.makeIntegerTextField().apply()
        {
            text = model?.newPosition?.x?.toString() ?: 0.toString()
        }

        val initialYPositionTextField = ValidatableTextField.makeIntegerTextField().apply()
        {
            text = model?.newPosition?.y?.toString() ?: 0.toString()
        }

        val directionComboBox = ComboBox<NamedValue<Behaviour.CardinalDirection>>().apply()
        {
            Behaviour.CardinalDirection.values().forEach()
            {
                items.add(NamedValue(it.friendly,it))
            }
            value = items.find {it.value == model?.newDirection} ?: items.first()
        }

        val jumpToInitialPositionCheckBox = CheckBox().apply()
        {
            isSelected = model?.shouldJumpToPosition ?: true
            val listener = InvalidationListener()
            {
                initialXPositionTextField.isDisable = !isSelected
                initialYPositionTextField.isDisable = !isSelected
                directionComboBox.isDisable = !isSelected
            }
            selectedProperty().addListener(listener)
            listener.invalidated(selectedProperty())
        }

        val operatorInputPane = RevisionFunctionConfigPanel()
            .apply()
            {
                val beliefRevisionStrategy = model?.problemInstance?.beliefRevisionStrategy ?: return@apply
                setValuesFrom(beliefRevisionStrategy)
            }

        init
        {
            dialogPane.minWidth = DIALOG_WIDTH
            dialogPane.minHeight = DIALOG_HEIGHT
            isResizable = true

            title = if (model == null) DIALOG_TITLE_ADD else DIALOG_TITLE_EDIT
            headerText = " "
            dialogPane.content = VBox().apply()
            {
                spacing = Dimens.KEYLINE_SMALL.toDouble()
                children += Label(DIALOG_LABEL_CURRENT_K)
                children += initialKTextField
                children += Label(DIALOG_LABEL_TARGET_K)
                children += targetKTextField
                children += Label(DIALOG_LABEL_COLOR)
                children += colorComboBox
                children += Label(DIALOG_LABEL_SHOULD_JUMP)
                children += jumpToInitialPositionCheckBox
                children += Label(DIALOG_LABEL_JUMP_X)
                children += initialXPositionTextField
                children += Label(DIALOG_LABEL_JUMP_Y)
                children += initialYPositionTextField
                children += Label(DIALOG_LABEL_JUMP_DIRECTION)
                children += directionComboBox
                children += Label(DIALOG_LABEL_BELIEF_REVISION_OPERATOR)
                children += operatorInputPane
            }
            buttonTypes.addAll(ButtonType.CANCEL,ButtonType.OK)
        }
    }
}
