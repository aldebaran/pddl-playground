package com.softbankrobotics.pddlplayground.util

import android.content.Context
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import com.softbankrobotics.pddlplanning.Tasks
import com.softbankrobotics.pddlplayground.MainActivity.Companion.planSearchFunction
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService

object PDDLUtil {

    private const val OBJECT = "object"

    private val typeList = listOf(
        "human - object",
        "emotion - object"
    )
    private val constantList = listOf(
        "happy - emotion",
        "neutral - emotion",
        "sad - emotion"
    )
    private val predicateList = listOf(
        "feels ?p1 - human ?p2 - emotion",
        "is_around ?p1 - human",
        "is_greeted ?p1 - human"
    )
    private val actionList = listOf(
        "joke_with\n:parameters (\n?human1 - human\n)\n:precondition (and\n(is_around ?human1)\n)\n:effect (and\n(feels ?human1 happy)\n)\n",
        "greet\n:parameters (\n?human1 - human\n)\n:precondition (and\n(is_around ?human1)\n)\n:effect (and\n(is_greeted ?human1)\n)\n",
        "find_human\n:parameters (\n?human1 - human\n)\n:precondition (and\n)\n:effect (and\n(is_around ?human1)\n)\n"
    )
    private val objectList = listOf(
        "alice - human",
        "bob - human",
        "charles - human",
        "someone - human"
    )
    private val initList = listOf(
        "is_around charles",
        "feels alice neutral",
        "feels bob sad"
    )
    private val goalList = listOf(
        "forall (?h - human) (imply (is_around ?h) (feels ?h happy))",
        "forall (?h - human) (imply (is_around ?h) (is_greeted ?h))",
        "imply (not (exists(?h - human) (is_around ?h))) (is_around someone)"
    )

    private val sampleScenario = mapOf(
        Pair(PDDLCategory.TYPE.ordinal, typeList),
        Pair(PDDLCategory.CONSTANT.ordinal, constantList),
        Pair(PDDLCategory.PREDICATE.ordinal, predicateList),
        Pair(PDDLCategory.ACTION.ordinal, actionList),
        Pair(PDDLCategory.OBJECT.ordinal, objectList),
        Pair(PDDLCategory.INIT.ordinal, initList),
        Pair(PDDLCategory.GOAL.ordinal, goalList)
    )

    fun clearDatabase(context: Context) {
        DatabaseHelper.getInstance(context).deleteAllExpressions()
        LoadExpressionsService.launchLoadExpressionsService(context)
    }

    fun fillInDatabaseWithSample(context: Context) {
        DatabaseHelper.getInstance(context).apply {
            sampleScenario.forEach { (category, list) ->
                list.forEach { label ->
                    addExpression().also { id ->
                        updateExpression(
                            Expression(id, label, category, true)
                        )
                    }
                }
            }
        }
        LoadExpressionsService.launchLoadExpressionsService(context)
    }

    suspend fun getPlanFromDatabase(context: Context): Tasks {
        val domain = getDomainPDDLFromDatabase(context)
        val problem = getProblemPDDLFromDatabase(context)
        return planSearchFunction(domain, problem, null)
    }

    fun getDomainPDDLFromDatabase(context: Context): String {
        val expressions = DatabaseHelper.getInstance(context).getExpressions()
            .filter { it.isEnabled() } // only retrieved enabled expressions
        val types = expressions
            .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
            .map { it.getLabel() }
        val constants = expressions
            .filter { it.getCategory() == PDDLCategory.CONSTANT.ordinal }
            .map { it.getLabel() }
        val predicates = expressions
            .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
            .map { it.getLabel() }
        val actions = expressions
            .filter { it.getCategory() == PDDLCategory.ACTION.ordinal }
            .map { it.getLabel() }
        return toDomain(
            types, constants, predicates, actions
        )
    }

    fun getProblemPDDLFromDatabase(context: Context): String {
        val expressions = DatabaseHelper.getInstance(context).getExpressions()
            .filter { it.isEnabled() } // only retrieved enabled expressions
        val objects = expressions
            .filter { it.getCategory() == PDDLCategory.OBJECT.ordinal }
            .map { it.getLabel() }
        val inits = expressions
            .filter { it.getCategory() == PDDLCategory.INIT.ordinal }
            .map { it.getLabel() }
        val goals = expressions
            .filter { it.getCategory() == PDDLCategory.GOAL.ordinal }
            .map { it.getLabel() }
        return toProblem(
            objects, inits, goals
        )
    }

    private fun toDomain(types: List<String?>,
                         constants: List<String?>,
                         predicates: List<String?>,
                         actions: List<String?>): String {
        var domain = "(define (domain playground_domain)\n" +
                "  (:requirements :adl)\n"
        // types
        domain += "  (:types\n"
        for (type in types)
            domain += "    $type\n"
        domain += "  )\n"
        // constants
        domain += "  (:constants\n"
        for (constant in constants)
            domain += "    $constant\n"
        domain += "  )\n"
        // predicates
        domain += "  (:predicates\n"
        for (predicate in predicates)
            domain += "    ($predicate)\n"
        domain += "  )\n"
        // actions
        for (action in actions)
            domain += "  (:action $action)\n"
        // close domain
        domain += ")\n"
        return domain
    }

    private fun toProblem(objects: List<String?>,
                          inits: List<String?>,
                          goals: List<String?>): String {
        var problem = "(define (problem my_problem)\n" +
                "   (:domain playground_domain)\n" +
                "   (:requirements :adl)\n"
        // objects
        problem += "  (:objects\n"
        for (obj in objects)
            problem += "    $obj\n"
        problem += "  )\n"
        // inits
        problem += "  (:init\n"
        for (init in inits)
            problem += "    ($init)\n"
        problem += "  )\n"
        // goals
        problem += "  (:goal\n    (and\n"
        for (goal in goals)
            problem += "      ($goal)\n"
        problem += "    )\n  )\n"
        // close problem
        problem += ")\n"
        return problem
    }

    fun getPredicates(context: Context): List<String?> {
        return DatabaseHelper.getInstance(context).getExpressions()
            .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
            .map { it.getLabel() }
            .map { it?.substringBefore(' ') }
    }

    fun getTypes(context: Context): List<String?> {
        return DatabaseHelper.getInstance(context).getExpressions()
            .asSequence()
            .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
            .map { it.getLabel() }
            .map { it?.substringBefore(" - ") }
            .filter { !it.isNullOrEmpty() }
            .plus(OBJECT)
            .toList()
    }

    fun getSubtypes(type: String?, context: Context): List<String> {
        if (type.isNullOrEmpty())
            return emptyList()
        val types = DatabaseHelper.getInstance(context).getExpressions()
            .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
            .map { it.getLabel() }
        return getSubtypes(type, types)
    }

    private fun getSubtypes(type: String, types: List<String?>): List<String> {
        val subtypeList = mutableListOf<String>()
        val children = types.filter {
            it?.substringAfter(" - ") == type
        }.map {
            it?.substringBefore(" - ")
        }
        return if (children.isNullOrEmpty())
            listOf(type)
        else {
            for (child in children) {
                if (!child.isNullOrEmpty())
                    subtypeList.addAll(getSubtypes(child, types))
            }
            subtypeList.plus(type)
        }
    }

    fun isObjectOfType(objectLabel: String?, type: String?, context: Context): Boolean {
        if (objectLabel.isNullOrEmpty() || type.isNullOrEmpty())
            return false
        return if (objectLabel.substringAfter(" - ") == type) // simple case
            true
        else { // check parent type
            val types = DatabaseHelper.getInstance(context).getExpressions()
                .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
                .map { it.getLabel() }
            val objectType = objectLabel.substringAfter(" - ")
            isOfType(objectType, type, types)
        }
    }

    private fun isOfType(child: String, type: String, types: List<String?>): Boolean {
        val parent = types.first {
            it?.substringBefore( " - ") == child
        }?.substringAfter(" - ")
        return if (parent == type)
            true
        else if (parent == "object")
            false
        else if (!parent.isNullOrEmpty())
            isOfType(parent, type, types)
        else false
    }

    fun getParamsForPredicates(
            checkBoxes: MutableList<CheckBox>,
            texts: MutableList<EditText>,
            paramSpinners: MutableList<Spinner>,
            context: Context,
            goal: Boolean = false
    ): Map<String, List<Set<String?>>> {
        val predicates = DatabaseHelper.getInstance(context).getExpressions()
            .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
            .map { it.getLabel() }
        val paramLabels = mutableMapOf<String, List<Set<String?>>>()
        val allTypeLabels = getTypes(context)
        for (predicate in predicates) {
            val predicateLabel = predicate?.substringBefore(' ')
            val type = predicate?.substringAfter(" - ")?.substringBefore(" ")
            val constsAndParam = mutableListOf<String?>()
            val constsAndParam2 = mutableListOf<String?>()

            if (!type.isNullOrEmpty() && allTypeLabels.any { it == type }) { // if it's actually a type
                val consts = DatabaseHelper.getInstance(context).getExpressions()
                    .filter { it.getCategory() == PDDLCategory.CONSTANT.ordinal }
                    .map { it.getLabel() }
                val objects = DatabaseHelper.getInstance(context).getExpressions()
                    .filter { it.getCategory() == PDDLCategory.OBJECT.ordinal }
                    .map { it.getLabel() }
                val objs = if (goal) {
                    consts.plus(objects)
                } else { consts }
                val suitableTypes = getSubtypes(type, context)
                for (suitableType in suitableTypes) {
                    val consts1 = objs.filter {
                            isObjectOfType(it, suitableType, context)
                        }.map { it?.substringBefore(' ') }
                    constsAndParam.addAll(consts1)
                    // loop through parameters, add to label
                    for ((index, checkBox) in checkBoxes.withIndex()) {
                        if (checkBox.isChecked) {
                            if ((paramSpinners[index].selectedItem as String) == suitableType) {
                                constsAndParam.add("?${texts[index].text}")
                            }
                        }
                    }
                }
                val type2 = predicate.substringAfter(type)
                    .substringAfter(" - ").substringBefore(" ")
                if (type2.isNotEmpty() && allTypeLabels.any { it == type2 }) {
                    val suitableTypes2 = getSubtypes(type2, context)
                    for (suitableType2 in suitableTypes2) {
                        val consts2 = objs.filter {
                            isObjectOfType(it, suitableType2, context)
                        }.map { it?.substringBefore(' ') }
                        constsAndParam2.addAll(consts2)
                        // loop through parameters, add to label
                        for ((index, checkBox) in checkBoxes.withIndex()) {
                            if (checkBox.isChecked) {
                                if ((paramSpinners[index].selectedItem as String) == suitableType2) {
                                    constsAndParam2.add("?${texts[index].text}")
                                }
                            }
                        }
                    }
                }
            }
            paramLabels[predicateLabel!!] = listOf(constsAndParam.toSet(), constsAndParam2.toSet())
        }
        return paramLabels
    }

    fun getParamsForGoals(
        checkBoxes: MutableList<CheckBox>,
        texts: MutableList<EditText>,
        paramSpinners: MutableList<Spinner>
    ): List<String> {
        val paramList = mutableListOf<String>()

        for ((index, checkBox) in checkBoxes.withIndex()) {
            if (checkBox.isChecked) {
                paramList.add("?${texts[index].text} - ${(paramSpinners[index].selectedItem as String)}")
            }
        }
        return paramList
    }
}