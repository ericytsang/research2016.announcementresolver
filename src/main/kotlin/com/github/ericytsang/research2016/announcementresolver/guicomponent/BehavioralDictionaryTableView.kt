package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.github.ericytsang.lib.collections.ObservableList
import com.github.ericytsang.lib.javafxutils.PolymorphicComboBox
import com.github.ericytsang.lib.javafxutils.EditableTableView
import com.github.ericytsang.lib.javafxutils.JavafxUtils
import com.github.ericytsang.lib.javafxutils.ValidatableTextField
import com.github.ericytsang.research2016.announcementresolver.simulation.Behaviour
import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.makeFrom
import com.github.ericytsang.research2016.propositionallogic.toParsableString
import com.sun.javafx.collections.ObservableListWrapper
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

/**
 * Created by surpl on 8/13/2016.
 */
class BehavioralDictionaryTableView:EditableTableView<BehavioralDictionaryTableView.RowData>()
{
    companion object
    {
        /**
         * names of columns in the table view.
         */
        private const val COLUMN_NAME_PROPOSITIONS:String = "Proposition"
        private const val COLUMN_NAME_BEHAVIOURS:String = "Behaviour"
    }

    init
    {
        // add columns to the table view
        columns.add(TableColumn<RowData,String>().apply()
        {
            text = COLUMN_NAME_PROPOSITIONS
            cellValueFactory = Callback<TableColumn.CellDataFeatures<RowData,String>,ObservableValue<String>>()
            {
                SimpleStringProperty(it.value.proposition.toString())
            }
        })
        columns.add(TableColumn<RowData,String>().apply()
        {
            text = COLUMN_NAME_BEHAVIOURS
            cellValueFactory = Callback<TableColumn.CellDataFeatures<RowData,String>,ObservableValue<String>>()
            {
                SimpleStringProperty(it.value.behavior.toString())
            }
        })

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
                return RowData(
                    Proposition.makeFrom(inputDialog.variableTextField.text),
                    inputDialog.behaviorComboBox.product!!)
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
     * holds the data for a single row in [BehavioralDictionaryTableView].
     */
    data class RowData(val proposition:Proposition,val behavior:Behaviour)

    /**
     * used to gather user input to create or edit [RowData] instances.
     */
    private class InputDialog(model:RowData?):Alert(AlertType.NONE)
    {
        val variableTextField = TextField().apply()
        {
            // set control value to reflect model data
            text = model?.proposition?.toParsableString() ?: ""
        }

        val behaviorComboBox = PolymorphicComboBox<PolymorphicComboBox.Option<Behaviour>,Behaviour>().apply()
        {
            // configure control...
            spacing = Dimens.KEYLINE_SMALL.toDouble()
            comboBox.items.addAll(
                DoNothingBehaviorOption(),
                WanderBehaviorOption(),
                GuardBehaviorOption(),
                PatrolBehaviorOption())

            // set control value to reflect model data
            product = model?.behavior
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
                children += Label("Proposition:")
                children += variableTextField
                children += Label("Behaviour:")
                children += behaviorComboBox
            }
        }

        private class DoNothingBehaviorOption():PolymorphicComboBox.Option<Behaviour>
        {
            override val panel:Node? = null
            override fun build() = Behaviour.DoNothing()
            override fun parse(product:Behaviour)
            {
                product as Behaviour.DoNothing
            }
            override fun toString():String = "Do Nothing"
        }

        private class WanderBehaviorOption():PolymorphicComboBox.Option<Behaviour>
        {
            override val panel:Node? = null
            override fun build() = Behaviour.Wander()
            override fun parse(product:Behaviour)
            {
                product as Behaviour.Wander
            }
            override fun toString():String = "Wander"
        }

        private class GuardBehaviorOption():PolymorphicComboBox.Option<Behaviour>
        {
            val xPositionTextField = ValidatableTextField.makeIntegerTextField()
            val yPositionTextField = ValidatableTextField.makeIntegerTextField()
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

            override fun parse(product:Behaviour)
            {
                product as Behaviour.Guard
                xPositionTextField.text = product.x.toString()
                yPositionTextField.text = product.y.toString()
                directionComboBox.value = product.direction
            }

            override fun toString():String = "Guard"
        }

        private class PatrolBehaviorOption():PolymorphicComboBox.Option<Behaviour>
        {
            override val panel = object:EditableTableView<Behaviour.Guard>()
            {
                private val DIALOG_TITLE_ADD:String = "Add New Waypoint"
                private val DIALOG_TITLE_EDIT:String = "Edit Existing Waypoint"

                init
                {
                    // add columns to the table view
                    columns += TableColumn<Behaviour.Guard,String>().apply()
                    {
                        text = "x"
                        cellValueFactory = Callback()
                        {
                            SimpleStringProperty(it.value.x.toString())
                        }
                    }
                    columns += TableColumn<Behaviour.Guard,String>().apply()
                    {
                        text = "y"
                        cellValueFactory = Callback()
                        {
                            SimpleStringProperty(it.value.y.toString())
                        }
                    }
                    columns += TableColumn<Behaviour.Guard,String>().apply()
                    {
                        text = "direction"
                        cellValueFactory = Callback()
                        {
                            SimpleStringProperty(it.value.direction.friendly)
                        }
                    }

                    // set the column resize policy
                    columnResizePolicy = CONSTRAINED_RESIZE_POLICY
                }

                override fun createOrUpdateItem(previousInput:Behaviour.Guard?):Behaviour.Guard?
                {
                    val inputDialog = InputDialog(previousInput)
                    while (inputDialog.showAndWait().get() == ButtonType.OK)
                    {
                        try
                        {
                            // todo: better error messages

                            val xPosition = inputDialog.xPositionTextField.text.toInt()
                            val yPosition = inputDialog.yPositionTextField.text.toInt()
                            val direction = inputDialog.directionComboBox.value

                            return Behaviour.Guard(xPosition,yPosition,direction)
                        }
                        catch (ex:Exception)
                        {
                            JavafxUtils.showErrorDialog("Invalid Input","Invalid input format.",ex)
                        }
                    }
                    return null
                }

                private inner class InputDialog(model:Behaviour.Guard?):Alert(AlertType.NONE)
                {
                    val xPositionTextField = ValidatableTextField.makeIntegerTextField()
                        .apply {text = model?.x?.toString() ?: ""}
                    val yPositionTextField = ValidatableTextField.makeIntegerTextField()
                        .apply {text = model?.y?.toString() ?: ""}
                    val directionComboBox = ComboBox<Behaviour.CardinalDirection>().apply()
                    {
                        items.addAll(Behaviour.CardinalDirection.values())
                        value = model?.direction
                    }

                    init
                    {
                        isResizable = true
                        title = if (model == null) DIALOG_TITLE_ADD else DIALOG_TITLE_EDIT
                        headerText = " "
                        dialogPane.content = VBox().apply()
                        {
                            children += Label("X position:")
                            children += xPositionTextField
                            children += Label("Y position:")
                            children += yPositionTextField
                            children += Label("Direction:")
                            children += directionComboBox
                        }
                        buttonTypes.addAll(ButtonType.CANCEL,ButtonType.OK)
                    }
                }
            }

            override fun build():Behaviour.Patrol
            {
                return Behaviour.Patrol(panel.items.toList())
            }

            override fun parse(product:Behaviour)
            {
                product as Behaviour.Patrol
                panel.items = ObservableListWrapper(product.waypoints.toMutableList())
            }

            override fun toString():String = "Patrol"
        }
    }
}
