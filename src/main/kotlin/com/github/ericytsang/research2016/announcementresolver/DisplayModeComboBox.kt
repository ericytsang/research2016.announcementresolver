package com.github.ericytsang.research2016.announcementresolver

import com.github.ericytsang.research2016.propositionallogic.And
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.Variable
import com.github.ericytsang.research2016.propositionallogic.and
import com.github.ericytsang.research2016.propositionallogic.not
import com.github.ericytsang.research2016.propositionallogic.or
import com.github.ericytsang.research2016.propositionallogic.tautology
import com.github.ericytsang.research2016.propositionallogic.toDnf
import com.github.ericytsang.research2016.propositionallogic.toFullDnf
import com.sun.javafx.collections.ObservableListWrapper
import javafx.scene.control.ComboBox

/**
 * Created by surpl on 7/15/2016.
 */
class DisplayModeComboBox:ComboBox<DisplayModeComboBox.Option>()
{
    companion object
    {
        /**
         * [List] of [Option]s used in the [displayModeComboBox] control.
         */
        private val options:List<Option> = run()
        {
            val cnfOption = Option("Disjunctive Normal Form",{it,unused -> it.map {it.toDnf().toString()}})
            val fullDnfOption = Option("Full Disjunctive Normal Form",{it,unused -> it.map {it.toFullDnf().toString()}})
            val defaultOption = Option("Default",{it,unused -> it.map {it.toString()}})
            val modelsOption = Option("Models")
            {
                it,variables ->
                if (it.isNotEmpty())
                {
                    val tautologies = variables.map {it or it.not}.let {And.make(it) ?: tautology}
                    (And.make(it)!! and tautologies).models.map {it.toString()}
                }
                else
                {
                    emptyList()
                }
            }
            return@run listOf(defaultOption,modelsOption,cnfOption,fullDnfOption)
        }
    }

    /**
     * [name] is displayed directly in the [displayModeComboBox] control.
     * [transform] is unused within this class.
     */
    class Option(val name:String,val transform:(List<Proposition>,Set<Variable>)->List<String>)
    {
        override fun toString():String = name
    }

    init
    {
        items = ObservableListWrapper(options)
        valueProperty().set(items.first())
    }
}
