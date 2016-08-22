package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.github.ericytsang.lib.collections.ConstrainedList
import com.github.ericytsang.lib.collections.Constraint
import com.github.ericytsang.lib.javafxutils.EditableTableView
import com.github.ericytsang.lib.javafxutils.ValidatableTextField
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import com.sun.javafx.collections.ObservableListWrapper
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.layout.VBox
import javafx.util.Callback
import java.util.ArrayList

/**
 * Created by surpl on 8/15/2016.
 */
class ObstacleTableView:EditableTableView<ObstacleTableView.RowData>()
{
    init
    {
        items = ArrayList<RowData>().let()
        {
            ConstrainedList(it).apply()
            {
                constraints += Constraint.new<List<RowData>>().apply()
                {
                    description = "all walls must have distinct locations"
                    isConsistent = Constraint.Predicate.new()
                    {
                        it.newValue.toSet().size == it.newValue.size
                    }
                }
            }
        }.let {ObservableListWrapper(it)}

        columns += TableColumn<RowData,String>().apply()
        {
            text = "Obstacles"
            cellValueFactory = Callback()
            {
                it.value.toString().let {SimpleStringProperty(it)}
            }
        }

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

                // todo: check that the cells are adjacent to one another...maybe can check in the init method of rowdata
                return RowData(
                    Simulation.Cell.getElseMake(inputDialog.position1XTextField.text.toInt(),inputDialog.position1YTextField.text.toInt()),
                    Simulation.Cell.getElseMake(inputDialog.position2XTextField.text.toInt(),inputDialog.position2YTextField.text.toInt()))
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

    data class RowData(val cell1:Simulation.Cell,val cell2:Simulation.Cell)

    private class InputDialog(val previousInput:RowData?):Alert(AlertType.NONE)
    {
        val position1XTextField = ValidatableTextField.makeIntegerTextField()
            .apply {text = previousInput?.cell1?.x?.toString() ?: ""}

        val position1YTextField = ValidatableTextField.makeIntegerTextField()
            .apply {text = previousInput?.cell1?.y?.toString() ?: ""}

        val position2XTextField = ValidatableTextField.makeIntegerTextField()
            .apply {text = previousInput?.cell2?.x?.toString() ?: ""}

        val position2YTextField = ValidatableTextField.makeIntegerTextField()
            .apply {text = previousInput?.cell2?.y?.toString() ?: ""}

        init
        {
            dialogPane.minWidth = 200.0
            dialogPane.minHeight = 200.0
            isResizable = true
            headerText = " "
            buttonTypes.addAll(ButtonType.CANCEL,ButtonType.OK)
            dialogPane.content = VBox().apply()
            {
                spacing = Dimens.KEYLINE_SMALL.toDouble()
                children += Label("Position 1 x:")
                children += position1XTextField
                children += Label("Position 1 y:")
                children += position1YTextField
                children += Label("Position 2 x:")
                children += position2XTextField
                children += Label("Position 2 y:")
                children += position2YTextField
            }
        }
    }
}
