package com.github.ericytsang.research2016.announcementresolver.guicomponent

import com.sun.javafx.collections.ObservableListWrapper
import javafx.beans.InvalidationListener
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.TextInputDialog
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import com.github.ericytsang.lib.collections.getRandom
import com.github.ericytsang.lib.javafxutils.EditableListView
import com.github.ericytsang.research2016.beliefrevisor.gui.Dimens
import com.github.ericytsang.research2016.propositionallogic.BeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.ComparatorBeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.HammingDistanceComparator
import com.github.ericytsang.research2016.propositionallogic.Or
import com.github.ericytsang.research2016.propositionallogic.OrderedSetsComparator
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.SatisfiabilityBeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.State
import com.github.ericytsang.research2016.propositionallogic.Variable
import com.github.ericytsang.research2016.propositionallogic.WeightedHammingDistanceComparator
import com.github.ericytsang.research2016.propositionallogic.makeFrom
import com.github.ericytsang.research2016.propositionallogic.toParsableString
import java.io.Serializable
import java.util.Collections
import java.util.Optional

class RevisionFunctionConfigPanel():VBox()
{
    companion object
    {
        const val BUTTON_MAKE_RANDOMORDERED_SET = "Generate random order"
    }

    private val hammingDistanceRevisionOperatorOption = HammingDistanceOption()
    private val weightedHammingDistanceRevisionOperatorOption = WeightedHammingDistanceOption()
    private val setInclusionRevisionOperatorOption = SatisfiabilityOption()
    private val orderedSetsRevisionOperatorOption = OrderedSetsOption()

    var listener:InvalidationListener? = null

    /**
     * [List] of [Option]s used in the
     * [revisionOperatorComboBox] control.
     */
    val options:List<Option> = run()
    {
        return@run listOf(hammingDistanceRevisionOperatorOption,
            weightedHammingDistanceRevisionOperatorOption,
            setInclusionRevisionOperatorOption,
            orderedSetsRevisionOperatorOption)
    }

    /**
     * returns a [Comparator] that can be used for belief revision.
     */
    val beliefRevisionStrategy:BeliefRevisionStrategy?
        get() = revisionOperatorComboBox.value.beliefRevisionStrategy

    /**
     * [revisionOperatorComboBox] is used by the user to select which revision
     * operator they would like to use.
     */
    val revisionOperatorComboBox = ComboBox<Option>()
        .apply()
        {
            items = ObservableListWrapper(options)
            value = items.first()
            valueProperty().addListener()
            {
                observableValue,oldValue,newValue ->
                oldValue.settingsPanel?.let()
                {
                    this@RevisionFunctionConfigPanel.children.remove(oldValue.settingsPanel)
                }
                newValue.settingsPanel?.let()
                {
                    this@RevisionFunctionConfigPanel.children.add(it)
                }
                listener?.invalidated(null)
            }
        }

    init
    {
        spacing = Dimens.KEYLINE_SMALL.toDouble()
        children.addAll(revisionOperatorComboBox)
    }

    fun setValuesFrom(beliefRevisionStrategy:BeliefRevisionStrategy)
    {
        when (beliefRevisionStrategy)
        {
            is SatisfiabilityBeliefRevisionStrategy ->
            {
                revisionOperatorComboBox.value = setInclusionRevisionOperatorOption
            }
            is ComparatorBeliefRevisionStrategy ->
            {
                val comparator = beliefRevisionStrategy.situationSorterFactory(emptySet())
                when (comparator)
                {
                    is HammingDistanceComparator ->
                    {
                        revisionOperatorComboBox.value = hammingDistanceRevisionOperatorOption
                    }
                    is WeightedHammingDistanceComparator ->
                    {
                        revisionOperatorComboBox.value = weightedHammingDistanceRevisionOperatorOption
                        weightedHammingDistanceRevisionOperatorOption.settingsPanel.items = comparator.weights.entries
                            .map {Mapping(it.key.friendly,it.value)}
                            .let {ObservableListWrapper(it)}
                    }
                    is OrderedSetsComparator ->
                    {
                        revisionOperatorComboBox.value = orderedSetsRevisionOperatorOption
                        orderedSetsRevisionOperatorOption.listview.items = comparator.orderedSets
                            .let {ObservableListWrapper(it)}
                    }
                    else -> throw IllegalArgumentException("unknown beliefRevisionStrategy: $beliefRevisionStrategy")
                }
            }
            else -> throw IllegalArgumentException("unknown beliefRevisionStrategy: $beliefRevisionStrategy")
        }
    }

    abstract class Option(val name:String)
    {
        abstract val settingsPanel:Node?
        abstract val beliefRevisionStrategy:BeliefRevisionStrategy?
        override fun toString():String = name
    }

    private inner class HammingDistanceOption:Option("Hamming Distance")
    {
        override val settingsPanel:Node? = null
        override val beliefRevisionStrategy:BeliefRevisionStrategy
            get()
            {
                return ComparatorBeliefRevisionStrategy()
                {
                    initialBeliefState:Set<Proposition> ->
                    HammingDistanceComparator(initialBeliefState)
                }
            }
    }

    private inner class WeightedHammingDistanceOption:Option("Weighted Hamming Distance")
    {
        override val settingsPanel = object:EditableListView<Mapping>()
        {
            init
            {
                setVgrow(this,Priority.ALWAYS)
            }

            override fun createItem(previousInput:Mapping?):Mapping?
            {
                val inputDialog = makeInputDialog(previousInput)
                while (!isInputCancelled(inputDialog.showAndWait()))
                {
                    try
                    {
                        return tryParseInput(inputDialog)
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

            fun tryParseInput(inputDialog:TextInputDialog):Mapping
            {
                val subStrings = inputDialog.result.split("=")
                if (subStrings.size != 2)
                {
                    throw IllegalArgumentException("an equals sign must be used to represent the mapping e.g. \"a = 5\"")
                }
                if (!(subStrings[0].trim().matches(Regex("[a-zA-Z]+"))))
                {
                    throw IllegalArgumentException("the variable name \"${subStrings[0].trim()}\" may only contain alphabetic characters.")
                }
                if (!(subStrings[1].trim().matches(Regex("[0-9]+"))))
                {
                    throw IllegalArgumentException("the variable weight \"${subStrings[1].trim()}\" may only contain numeric characters.")
                }
                val variableName = subStrings[0].trim()
                val weight = subStrings[1].trim().toInt()
                return Mapping(variableName,weight)
            }

            override fun isConsistent(items:List<Mapping>):List<String>
            {
                val violatedConstraints = mutableListOf<String>()
                if (items.map {it.variableName}.toSet().size != items.map {it.variableName}.size)
                {
                    violatedConstraints += "each variable may only be mapped once"
                }
                return violatedConstraints
            }

            fun makeInputDialog(model:Mapping?):TextInputDialog
            {
                return TextInputDialog(model?.toString())
                    .apply {headerText = "Enter the mapping below"}
            }

            fun isInputCancelled(result:Optional<String>):Boolean
            {
                return !result.isPresent
            }
        }

        override val beliefRevisionStrategy:BeliefRevisionStrategy get()
        {
            return ComparatorBeliefRevisionStrategy()
            {
                initialBeliefState:Set<Proposition> ->
                val weights = settingsPanel.items.associate {Variable.Companion.fromString(it.variableName) to it.weight}
                WeightedHammingDistanceComparator(initialBeliefState,weights)
            }
        }
    }

    class Mapping(val variableName:String,val weight:Int):Serializable
    {
        override fun toString():String = "$variableName = $weight"
    }

    private inner class SatisfiabilityOption:Option("Satisfiability")
    {
        override val settingsPanel = null
        override val beliefRevisionStrategy:BeliefRevisionStrategy = SatisfiabilityBeliefRevisionStrategy()
    }

    private inner class OrderedSetsOption:Option("Ordered Sets")
    {
        val listview = object:EditableListView<Proposition>()
        {
            override fun createItem(previousInput:Proposition?):Proposition?
            {
                val inputDialog = makeInputDialog(previousInput)
                while (!isInputCancelled(inputDialog.showAndWait()))
                {
                    try
                    {
                        return tryParseInput(inputDialog)
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

            fun isInputCancelled(result:Optional<String>):Boolean
            {
                return !result.isPresent
            }

            fun tryParseInput(inputDialog:TextInputDialog):Proposition
            {
                return Proposition.makeFrom(inputDialog.result)
            }

            fun makeInputDialog(model:Proposition?):TextInputDialog
            {
                return TextInputDialog(model?.toParsableString())
                    .apply()
                    {
                        headerText = "Enter the sentence below."
                    }
            }
        }

        val makeRandomButton = Button(BUTTON_MAKE_RANDOMORDERED_SET).apply()
        {
            onAction = EventHandler()
            {
                val dialogTitles = "Generate Random Ordering"
                val variablesPrompt = "Enter a comma separated list of all the variables below."
                val variablesParser = {string:String -> string.split(",").map {Variable.Companion.fromString(it.trim())}.toSet()}
                val numBucketsPrompt = "Enter the number of buckets to sort generated states into."
                val numBucketsParser = {string:String -> string.toInt()}

                // get input from user
                val variables:Set<Variable> = getInputByTextInputDialog(dialogTitles,variablesPrompt,variablesParser) ?: return@EventHandler
                val numBuckets:Int = getInputByTextInputDialog(dialogTitles,numBucketsPrompt,numBucketsParser) ?: return@EventHandler

                // randomized list of all possible states involving all
                // variables in variables represented by variable
                // conjunctions.
                val allStates = State.Companion.permutationsOf(variables)
                    .map {Proposition.Companion.fromState(it)}
                    .toMutableList()
                    .apply {Collections.shuffle(this)}
                    .iterator()

                // create numBucket dnf sentences that are  elements in
                // allStates concatenated with OR operators.
                val dnfSentences = Array<MutableSet<Proposition>>(numBuckets,{mutableSetOf()})
                    .apply()
                    {
                        for (i in indices)
                        {
                            if (allStates.hasNext())
                            {
                                this[i].add(allStates.next())
                            }
                            else
                            {
                                break
                            }
                        }
                        allStates.forEach {getRandom().add(it)}
                    }
                    .filter {it.isNotEmpty()}.map {Or.Companion.make(it.toList())}

                // add all the dnf sentences to the listView.
                listview.items.clear()
                listview.items.addAll(dnfSentences)
            }
        }

        override val settingsPanel = VBox().apply()
        {
            setVgrow(this,Priority.ALWAYS)
            spacing = Dimens.KEYLINE_SMALL.toDouble()
            children.addAll(listview,makeRandomButton)
        }

        init
        {
            listview.focusModel.focusedItemProperty().addListener(InvalidationListener()
            {
                listener?.invalidated(null)
            })
        }

        override val beliefRevisionStrategy:BeliefRevisionStrategy? get()
        {
            if (listview.items.isNotEmpty())
            {
                return ComparatorBeliefRevisionStrategy()
                {
                    initialBeliefState:Set<Proposition> ->
                    OrderedSetsComparator(initialBeliefState,listview.items)
                }
            }
            else
            {
                return null
            }
        }
    }

    private fun <R> getInputByTextInputDialog(dialogTitle:String,promptText:String,parseInput:(String)->R):R?
    {
        val textInputDialog = TextInputDialog().apply()
        {
            title = dialogTitle
            headerText = promptText
        }

        while (true)
        {
            // get the variables input
            val result = textInputDialog.showAndWait()

            // abort if the operation is cancelled
            if (!result.isPresent)
            {
                return null
            }

            // try to parse the input
            try
            {
                return parseInput(result.get())
            }

            // continue if parsing fails
            catch (ex:Exception)
            {
                Alert(Alert.AlertType.ERROR).apply()
                {
                    title = dialogTitle
                    headerText = "Failed to parse text input."
                    contentText = ex.message
                    showAndWait()
                }
                continue
            }
        }
    }
}
