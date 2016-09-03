package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.github.ericytsang.lib.constrainedlist.ConstrainedList
import com.github.ericytsang.lib.constrainedlist.Constraint
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
        // constrain what walls are allowed to be added
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

        // add columns
        columns += TableColumn<RowData,String>().apply()
        {
            text = "x1"
            cellValueFactory = Callback()
            {
                SimpleStringProperty(it.value.cell1.x.toString())
            }
        }
        columns += TableColumn<RowData,String>().apply()
        {
            text = "y1"
            cellValueFactory = Callback()
            {
                SimpleStringProperty(it.value.cell1.y.toString())
            }
        }
        columns += TableColumn<RowData,String>().apply()
        {
            text = "x2"
            cellValueFactory = Callback()
            {
                SimpleStringProperty(it.value.cell2.x.toString())
            }
        }
        columns += TableColumn<RowData,String>().apply()
        {
            text = "y2"
            cellValueFactory = Callback()
            {
                SimpleStringProperty(it.value.cell2.y.toString())
            }
        }

        // configure column resize policy
        columnResizePolicy = CONSTRAINED_RESIZE_POLICY
    }

    override fun createOrUpdateItem(previousInput:RowData?):RowData?
    {
        val inputDialog = InputDialog(previousInput)
        while (inputDialog.showAndWait().get() == ButtonType.OK)
        {
            try
            {
                val position1X = try
                {
                    inputDialog.position1XTextField.text.toInt()
                }
                catch (ex:Exception)
                {
                    throw RuntimeException("Invalid input for \"Position 1 x\". Could not parse \"${inputDialog.position1XTextField.text}\" into a signed integer.",ex)
                }

                val position1Y = try
                {
                    inputDialog.position1YTextField.text.toInt()
                }
                catch (ex:Exception)
                {
                    throw RuntimeException("Invalid input for \"Position 1 y\". Could not parse \"${inputDialog.position1YTextField.text}\" into a signed integer.",ex)
                }

                val position2X = try
                {
                    inputDialog.position2XTextField.text.toInt()
                }
                catch (ex:Exception)
                {
                    throw RuntimeException("Invalid input for \"Position 2 x\". Could not parse \"${inputDialog.position2XTextField.text}\" into a signed integer.",ex)
                }

                val position2Y = try
                {
                    inputDialog.position2YTextField.text.toInt()
                }
                catch (ex:Exception)
                {
                    throw RuntimeException("Invalid input for \"Position 2 y\". Could not parse \"${inputDialog.position2YTextField.text}\" into a signed integer.",ex)
                }

                return RowData(
                    Simulation.Cell.getElseMake(position1X,position1Y),
                    Simulation.Cell.getElseMake(position2X,position2Y))
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
    {
        override fun toString():String
        {
            return "[ ${cell1.x} , ${cell1.y} ] , [ ${cell2.x} , ${cell2.y} ]"
        }
    }

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
