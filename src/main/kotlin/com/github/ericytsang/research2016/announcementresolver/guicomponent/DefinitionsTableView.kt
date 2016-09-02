package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.github.ericytsang.lib.javafxutils.PolymorphicComboBox
import com.github.ericytsang.lib.javafxutils.EditableTableView
import com.github.ericytsang.lib.javafxutils.JavafxUtils
import com.github.ericytsang.lib.javafxutils.NamedValue
import com.github.ericytsang.lib.javafxutils.ValidatableTextField
import com.github.ericytsang.research2016.announcementresolver.simulation.AgentController
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
import javafx.scene.paint.Color
import javafx.util.Callback

/**
 * Created by surpl on 8/13/2016.
 */
class DefinitionsTableView:EditableTableView<DefinitionsTableView.RowData>()
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
                val proposition = try
                {
                    Proposition.makeFrom(inputDialog.variableTextField.text)
                }
                catch (ex:Exception)
                {
                    throw RuntimeException("Invalid input for \"Proposition\". Could not parse \"inputDialog.variableTextField.text\" into a propositional sentence.")
                }

                val behaviour = inputDialog.behaviorComboBox.product
                    ?: throw RuntimeException("Please select an option.")

                return RowData(proposition,behaviour)
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
     * holds the data for a single row in [DefinitionsTableView].
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
                PatrolBehaviorOption(),
                HideBehaviorOption(),
                FollowBehaviorOption(),
                ProtectBehaviorOption())

            // set control value to reflect model data
            product = model?.behavior
        }

        init
        {
            title = if (model == null) "Add New Definition" else "Edit Existing Definition"
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

        private class HideBehaviorOption():PolymorphicComboBox.Option<Behaviour>
        {
            override val panel:Node? = null
            override fun build() = Behaviour.Hide()
            override fun parse(product:Behaviour)
            {
                product as Behaviour.Hide
            }
            override fun toString():String = "Hide"
        }

        private class FollowBehaviorOption():PolymorphicComboBox.Option<Behaviour>
        {
            private val agentColorComboBox = ComboBox<NamedValue<Color>>().apply()
            {
                items.addAll(AgentsTableView.AGENT_COLOR_OPTIONS)
            }

            override val panel = VBox().apply()
            {
                spacing = Dimens.KEYLINE_SMALL.toDouble()
                children += Label("Color of agent to follow:")
                children += agentColorComboBox
            }
            override fun build() = Behaviour.Follow(agentColorComboBox.value.value)
            override fun parse(product:Behaviour)
            {
                product as Behaviour.Follow
                agentColorComboBox.value = agentColorComboBox.items.find {it.value == product.agentColor}
            }
            override fun toString():String = "Follow"
        }

        private class ProtectBehaviorOption():PolymorphicComboBox.Option<Behaviour>
        {
            private val agentColorComboBox = ComboBox<NamedValue<Color>>().apply()
            {
                items.addAll(AgentsTableView.AGENT_COLOR_OPTIONS)
            }

            override val panel = VBox().apply()
            {
                spacing = Dimens.KEYLINE_SMALL.toDouble()
                children += Label("Color of agent to protect:")
                children += agentColorComboBox
            }
            override fun build() = Behaviour.Protect(agentColorComboBox.value.value)
            override fun parse(product:Behaviour)
            {
                product as Behaviour.Protect
                agentColorComboBox.value = agentColorComboBox.items.find {it.value == product.agentColor}
            }
            override fun toString():String = "Protect"
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
                            val xPosition = try
                            {
                                inputDialog.xPositionTextField.text.toInt()
                            }
                            catch (ex:Exception)
                            {
                                throw RuntimeException("Invalid input for \"X position\". Could not be parse \"${inputDialog.xPositionTextField.text}\" into a signed integer.")
                            }

                            val yPosition = try
                            {
                                inputDialog.yPositionTextField.text.toInt()
                            }
                            catch (ex:Exception)
                            {
                                throw RuntimeException("Invalid input for \"Y position\". Could not be parse \"${inputDialog.yPositionTextField.text}\" into a signed integer.")
                            }

                            val direction = inputDialog.directionComboBox.value
                                ?: throw RuntimeException("Please select an option.")

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
