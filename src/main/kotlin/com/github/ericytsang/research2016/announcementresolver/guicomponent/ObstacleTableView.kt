package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.github.ericytsang.lib.javafxutils.EditableTableView
import com.github.ericytsang.lib.javafxutils.ValidatableTextField
import com.github.ericytsang.research2016.announcementresolver.robot.Point
import com.github.ericytsang.research2016.announcementresolver.robot.Wall
import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.layout.VBox
import javafx.util.Callback

/**
 * Created by surpl on 8/15/2016.
 */
class ObstacleTableView:EditableTableView<Wall>()
{
    init
    {
        columns += TableColumn<Wall,String>().apply()
        {
            text = "Obstacles"
            cellValueFactory = Callback()
            {
                it.value.toString().let {SimpleStringProperty(it)}
            }
        }

        columnResizePolicy = CONSTRAINED_RESIZE_POLICY
    }

    override fun createOrUpdateItem(previousInput:Wall?):Wall?
    {
        val inputDialog = InputDialog(previousInput)
        while (inputDialog.showAndWait().get() == ButtonType.OK)
        {
            try
            {
                // todo: better error messages
                return Wall(
                    Point(inputDialog.position1XTextField.text.toInt(),inputDialog.position1YTextField.text.toInt()),
                    Point(inputDialog.position2XTextField.text.toInt(),inputDialog.position2YTextField.text.toInt()))
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

    override fun isConsistent(items:List<Wall>):List<String>
    {
        val violatedConstraints = mutableListOf<String>()
        if (items.toSet().size != items.size)
        {
            violatedConstraints += "all walls must have distinct locations"
        }
        return violatedConstraints
    }

    val walls:Set<Wall> get()
    {
        return items.toSet()
    }

    private class InputDialog(val previousInput:Wall?):Alert(AlertType.NONE)
    {
        val position1XTextField = ValidatableTextField.makeNumericTextField()
            .apply {text = previousInput?.position1?.x?.toString() ?: ""}

        val position1YTextField = ValidatableTextField.makeNumericTextField()
            .apply {text = previousInput?.position1?.y?.toString() ?: ""}

        val position2XTextField = ValidatableTextField.makeNumericTextField()
            .apply {text = previousInput?.position2?.x?.toString() ?: ""}

        val position2YTextField = ValidatableTextField.makeNumericTextField()
            .apply {text = previousInput?.position2?.y?.toString() ?: ""}

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
