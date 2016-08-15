package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.github.ericytsang.lib.javafxutils.ComplexComboBox
import com.github.ericytsang.lib.javafxutils.EditableTableView
import com.github.ericytsang.lib.javafxutils.ValidatableTextField
import com.github.ericytsang.research2016.announcementresolver.robot.Behaviour
import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import com.github.ericytsang.research2016.propositionallogic.Variable
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.util.Callback
import java.util.Optional

/**
 * Created by surpl on 8/13/2016.
 */
class BehavioralDictionaryTableView:EditableTableView<BehavioralDictionaryTableView.RowData,Alert,ButtonType>()
{
    init
    {
        // add columns to the table view
        columns.add(TableColumn<RowData,String>().apply()
        {
            text = "Variable"
            cellValueFactory = Callback<TableColumn.CellDataFeatures<RowData,String>,ObservableValue<String>>()
            {
                SimpleStringProperty(it.value.variable.toString())
            }
        })
        columns.add(TableColumn<RowData,String>().apply()
        {
            text = "Behaviour"
            prefWidth= 250.0
            cellValueFactory = Callback<TableColumn.CellDataFeatures<RowData,String>,ObservableValue<String>>()
            {
                SimpleStringProperty(it.value.behavior.toString())
            }
        })
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
        return RowData(
            Variable.fromString(inputDialog.variableTextField.text),
            inputDialog.behaviorComboBox.comboBox.value.build())
    }

    data class RowData(val variable:Variable,val behavior:Behaviour)

    private class InputDialog(model:RowData?):Alert(AlertType.NONE)
    {
        val variableTextField = TextField().apply()
        {
            // set control value to reflect model data
            text = model?.variable?.toString() ?: ""
        }

        val behaviorComboBox = ComplexComboBox<ComplexComboBox.OptionalBuilder<Behaviour>,Behaviour>().apply()
        {
            // configure control...
            spacing = Dimens.KEYLINE_SMALL.toDouble()
            val wanderBehaviorOption = WanderBehaviorOption()
            val guardBehaviorOption = GuardBehaviorOption()
            comboBox.items.addAll(wanderBehaviorOption,guardBehaviorOption)

            // set control value to reflect model data
            val behaviour = model?.behavior
            when (behaviour)
            {
                null -> {}
                is Behaviour.Wander ->
                {
                    comboBox.value = wanderBehaviorOption
                }
                is Behaviour.Guard ->
                {
                    comboBox.value = guardBehaviorOption
                }
            }
            comboBox.value?.configureBuilderFrom(behaviour!!)
        }

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
                children += Label("Variable:")
                children += variableTextField
                children += Label("Behaviour:")
                children += behaviorComboBox
            }
        }

        private class WanderBehaviorOption():ComplexComboBox.OptionalBuilder<Behaviour>
        {
            override val panel:Node? = null
            override fun build() = Behaviour.Wander()
            override fun configureBuilderFrom(product:Behaviour) = Unit
            override fun toString():String = "Wander"
        }

        private class GuardBehaviorOption():ComplexComboBox.OptionalBuilder<Behaviour>
        {
            val xPositionTextField = ValidatableTextField.makeNumericTextField()
            val yPositionTextField = ValidatableTextField.makeNumericTextField()
            val directionComboBox = ComboBox<Behaviour.CardinalDirection>().apply()
            {
                items.addAll(Behaviour.CardinalDirection.values())
            }

            override val panel = VBox().apply()
            {
                children += Label("X position:")
                children += xPositionTextField
                children += Label("Y position:")
                children += yPositionTextField
                children += Label("Direction:")
                children += directionComboBox
            }

            override fun build():Behaviour.Guard
            {
                val xPosition = try
                {
                    xPositionTextField.text.toInt()
                }
                catch (ex:NumberFormatException)
                {
                    throw IllegalStateException("No x position specified.")
                }
                val yPosition = try
                {
                    yPositionTextField.text.toInt()
                }
                catch (ex:NumberFormatException)
                {
                    throw IllegalStateException("No y position specified.")
                }
                val direction = try
                {
                    directionComboBox.value!!
                }
                catch (ex:KotlinNullPointerException)
                {
                    throw IllegalStateException("No direction position specified.")
                }
                return Behaviour.Guard(xPosition,yPosition,direction)
            }

            override fun configureBuilderFrom(product:Behaviour)
            {
                product as Behaviour.Guard
                xPositionTextField.text = product.x.toString()
                yPositionTextField.text = product.y.toString()
                directionComboBox.value = product.direction
            }

            override fun toString():String = "Guard"
        }
    }
}
